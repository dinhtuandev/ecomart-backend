package com.ecomart.service.impl;

import com.ecomart.config.PaymentConfig;
import com.ecomart.dto.request.SePayWebhookRequest;
import com.ecomart.dto.response.VNPayIpnResponse;
import com.ecomart.dto.response.VNPayReturnResponse;
import com.ecomart.entity.Order;
import com.ecomart.entity.PaymentTransaction;
import com.ecomart.entity.enums.PaymentGateway;
import com.ecomart.entity.enums.PaymentStatus;
import com.ecomart.entity.enums.PaymentTransactionStatus;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.exception.UnauthorizedException;
import com.ecomart.exception.UnprocessableEntityException;
import com.ecomart.repository.OrderRepository;
import com.ecomart.repository.PaymentTransactionRepository;
import com.ecomart.service.PaymentService;
import com.ecomart.util.VNPayUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    public static final Pattern ORDER_CODE_PATTERN = Pattern.compile("EM-\\d{8}-[A-Z0-9]{5}");

    private final PaymentConfig paymentConfig;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String createVNPayPaymentUrl(Order order, String paymentRef, String ipAddress) {
        PaymentConfig.VNPayProperties vnpay = paymentConfig.getVnpay();

        long amount = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        String createDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String expireDate = LocalDateTime.now().plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpay.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", paymentRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderCode());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpay.getReturnUrl());
        vnpParams.put("vnp_IpAddr", (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1");
        vnpParams.put("vnp_CreateDate", createDate);
        vnpParams.put("vnp_ExpireDate", expireDate);

        String queryString = VNPayUtil.buildQueryString(vnpParams);
        String secureHash = VNPayUtil.hashAllFields(vnpParams, vnpay.getHashSecret());

        return vnpay.getPayUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    @Override
    public String createSePayQrUrl(Order order, String paymentRef) {
        PaymentConfig.SePayProperties sepay = paymentConfig.getSepay();

        String encodedDes = URLEncoder.encode(order.getOrderCode(), StandardCharsets.UTF_8);
        String amountStr = order.getTotalAmount().toBigInteger().toString();

        return sepay.getQrUrlTemplate()
                .replace("{acc}", sepay.getAccountNumber())
                .replace("{bank}", sepay.getBank())
                .replace("{amount}", amountStr)
                .replace("{des}", encodedDes);
    }

    @Override
    @Transactional
    public VNPayIpnResponse processVNPayIpn(Map<String, String> vnpParams) {
        PaymentConfig.VNPayProperties vnpay = paymentConfig.getVnpay();

        // 1. Verify Checksum
        String secureHash = vnpParams.get("vnp_SecureHash");
        String calculatedHash = VNPayUtil.hashAllFields(vnpParams, vnpay.getHashSecret());
        if (secureHash == null || !secureHash.equalsIgnoreCase(calculatedHash)) {
            return new VNPayIpnResponse("97", "Invalid Checksum");
        }

        // 2. Verify vnp_TmnCode
        String tmnCode = vnpParams.get("vnp_TmnCode");
        if (tmnCode == null || !tmnCode.equals(vnpay.getTmnCode())) {
            return new VNPayIpnResponse("97", "Invalid Checksum");
        }

        // 3. Find PaymentTransaction strictly by paymentRef
        String paymentRef = vnpParams.get("vnp_TxnRef");
        if (paymentRef == null || paymentRef.isBlank()) {
            return new VNPayIpnResponse("01", "Order not Found");
        }

        Optional<PaymentTransaction> txOpt = paymentTransactionRepository.findByPaymentRef(paymentRef);
        if (txOpt.isEmpty()) {
            return new VNPayIpnResponse("01", "Order not Found");
        }

        PaymentTransaction tx = txOpt.get();
        Order order = tx.getOrder();

        // 4. Verify Amount vs PaymentTransaction snapshot amount
        String amountParam = vnpParams.get("vnp_Amount");
        long expectedAmount = tx.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        if (amountParam == null || Long.parseLong(amountParam) != expectedAmount) {
            return new VNPayIpnResponse("04", "Invalid Amount");
        }

        // 5. Idempotency Check
        if (order.getPaymentStatus() == PaymentStatus.PAID || tx.getStatus() == PaymentTransactionStatus.SUCCESS) {
            return new VNPayIpnResponse("02", "Order already confirmed");
        }

        // 6. Update status based on vnp_ResponseCode
        String responseCode = vnpParams.get("vnp_ResponseCode");
        String transactionNo = vnpParams.get("vnp_TransactionNo");
        String rawResponse = serializeJson(vnpParams);

        if ("00".equals(responseCode)) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            tx.setStatus(PaymentTransactionStatus.SUCCESS);
        } else {
            order.setPaymentStatus(PaymentStatus.UNPAID);
            tx.setStatus(PaymentTransactionStatus.FAILED);
        }

        tx.setGatewayTransactionNo(transactionNo);
        tx.setRawResponse(rawResponse);

        orderRepository.save(order);
        paymentTransactionRepository.save(tx);

        return new VNPayIpnResponse("00", "Confirm Success");
    }

    @Override
    public VNPayReturnResponse processVNPayReturn(Map<String, String> vnpParams) {
        PaymentConfig.VNPayProperties vnpay = paymentConfig.getVnpay();

        String secureHash = vnpParams.get("vnp_SecureHash");
        String calculatedHash = VNPayUtil.hashAllFields(vnpParams, vnpay.getHashSecret());
        boolean isValidChecksum = secureHash != null && secureHash.equalsIgnoreCase(calculatedHash);

        String paymentRef = vnpParams.get("vnp_TxnRef");
        String orderCode = extractOrderCodeFromPaymentRef(paymentRef);
        String responseCode = vnpParams.get("vnp_ResponseCode");
        String transactionNo = vnpParams.get("vnp_TransactionNo");
        String bankCode = vnpParams.get("vnp_BankCode");
        String cardType = vnpParams.get("vnp_CardType");
        String orderInfo = vnpParams.get("vnp_OrderInfo");
        String payDate = vnpParams.get("vnp_PayDate");

        BigDecimal amount = BigDecimal.ZERO;
        String amountParam = vnpParams.get("vnp_Amount");
        if (amountParam != null && !amountParam.isBlank()) {
            try {
                amount = new BigDecimal(amountParam).divide(BigDecimal.valueOf(100));
            } catch (Exception ignored) {
            }
        }

        if (!isValidChecksum) {
            return VNPayReturnResponse.builder()
                    .orderCode(orderCode)
                    .transactionNo(transactionNo)
                    .amount(amount)
                    .bankCode(bankCode)
                    .cardType(cardType)
                    .orderInfo(orderInfo)
                    .payDate(payDate)
                    .responseCode(responseCode)
                    .isSuccess(false)
                    .message("Chữ ký giao dịch không hợp lệ.")
                    .build();
        }

        boolean isSuccess = "00".equals(responseCode);
        String message = isSuccess ? "Thanh toán giao dịch thành công." : "Giao dịch thanh toán không thành công hoặc đã bị hủy.";

        return VNPayReturnResponse.builder()
                .orderCode(orderCode)
                .transactionNo(transactionNo)
                .amount(amount)
                .bankCode(bankCode)
                .cardType(cardType)
                .orderInfo(orderInfo)
                .payDate(payDate)
                .responseCode(responseCode)
                .isSuccess(isSuccess)
                .message(message)
                .build();
    }

    @Override
    @Transactional
    public void processSePayWebhook(String authHeader, SePayWebhookRequest request) {
        PaymentConfig.SePayProperties sepay = paymentConfig.getSepay();

        // 1. Verify Authorization Header
        String expectedHeader = "Apikey " + sepay.getApiKey();
        if (authHeader == null || !authHeader.trim().equals(expectedHeader)) {
            throw new UnauthorizedException("API Key của SePay không hợp lệ");
        }

        // 2. Validate referenceCode
        if (request.getReferenceCode() == null || request.getReferenceCode().isBlank()) {
            throw new UnprocessableEntityException("Mã tham chiếu giao dịch (referenceCode) không được để trống");
        }

        // 3. Validate accountNumber
        if (sepay.getAccountNumber() != null && !sepay.getAccountNumber().isBlank()
                && (request.getAccountNumber() == null || !request.getAccountNumber().equals(sepay.getAccountNumber()))) {
            throw new UnprocessableEntityException("Số tài khoản nhận tiền không khớp với cấu hình hệ thống");
        }

        // 4. Validate transferType
        if (request.getTransferType() == null || !request.getTransferType().equalsIgnoreCase("in")) {
            throw new UnprocessableEntityException("Loại giao dịch không hợp lệ, chỉ chấp nhận giao dịch tiền vào");
        }

        // 5. Idempotency Case A: Duplicate referenceCode
        if (paymentTransactionRepository.existsByGatewayAndGatewayTransactionNo(PaymentGateway.SEPAY, request.getReferenceCode())) {
            return;
        }

        // 6. Extract orderCode from content & description
        Set<String> orderCodes = new HashSet<>();
        if (request.getContent() != null) {
            Matcher m = ORDER_CODE_PATTERN.matcher(request.getContent());
            while (m.find()) {
                orderCodes.add(m.group());
            }
        }
        if (request.getDescription() != null) {
            Matcher m = ORDER_CODE_PATTERN.matcher(request.getDescription());
            while (m.find()) {
                orderCodes.add(m.group());
            }
        }

        if (orderCodes.size() > 1) {
            throw new UnprocessableEntityException("Phát hiện mã đơn hàng không đồng nhất trong nội dung chuyển khoản");
        }
        if (orderCodes.isEmpty()) {
            throw new UnprocessableEntityException("Không tìm thấy mã đơn hàng hợp lệ trong nội dung chuyển khoản");
        }

        String orderCode = orderCodes.iterator().next();

        // 7. Find Order
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại với mã: " + orderCode));

        // 8. Idempotency Case B: Order already PAID
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }

        // 9. Find PENDING SePay transaction strictly
        PaymentTransaction tx = paymentTransactionRepository
                .findFirstByOrderIdAndGatewayAndStatusOrderByCreatedAtDesc(order.getId(), PaymentGateway.SEPAY, PaymentTransactionStatus.PENDING)
                .orElseThrow(() -> new UnprocessableEntityException("Không tìm thấy giao dịch thanh toán SePay đang chờ xử lý cho đơn hàng này"));

        // 10. Validate Exact Amount
        if (request.getTransferAmount() == null || request.getTransferAmount().compareTo(tx.getAmount()) != 0) {
            if (request.getTransferAmount() != null && request.getTransferAmount().compareTo(tx.getAmount()) < 0) {
                throw new UnprocessableEntityException("Số tiền chuyển khoản không đủ (yêu cầu " + tx.getAmount() + ")");
            } else {
                throw new UnprocessableEntityException("Số tiền chuyển khoản vượt quá tổng tiền đơn hàng (yêu cầu " + tx.getAmount() + ")");
            }
        }

        // 11. Update states
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaidAt(LocalDateTime.now());

        tx.setGatewayTransactionNo(request.getReferenceCode());
        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setRawResponse(serializeJson(request));

        orderRepository.save(order);
        paymentTransactionRepository.save(tx);
    }

    private String extractOrderCodeFromPaymentRef(String paymentRef) {
        if (paymentRef == null) {
            return null;
        }
        Matcher matcher = ORDER_CODE_PATTERN.matcher(paymentRef);
        if (matcher.find()) {
            return matcher.group();
        }
        return paymentRef;
    }

    private String serializeJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
