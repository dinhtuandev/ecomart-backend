package com.ecomart.service;

import com.ecomart.config.PaymentConfig;
import com.ecomart.dto.request.SePayWebhookRequest;
import com.ecomart.dto.response.VNPayIpnResponse;
import com.ecomart.dto.response.VNPayReturnResponse;
import com.ecomart.entity.Order;
import com.ecomart.entity.PaymentTransaction;
import com.ecomart.entity.User;
import com.ecomart.entity.enums.OrderStatus;
import com.ecomart.entity.enums.PaymentGateway;
import com.ecomart.entity.enums.PaymentMethod;
import com.ecomart.entity.enums.PaymentStatus;
import com.ecomart.entity.enums.PaymentTransactionStatus;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.exception.UnauthorizedException;
import com.ecomart.exception.UnprocessableEntityException;
import com.ecomart.repository.OrderRepository;
import com.ecomart.repository.PaymentTransactionRepository;
import com.ecomart.service.impl.PaymentServiceImpl;
import com.ecomart.util.VNPayUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentConfig paymentConfig;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentConfig.VNPayProperties vnpayProps;
    private PaymentConfig.SePayProperties sepayProps;
    private Order order;
    private PaymentTransaction vnpayTx;
    private PaymentTransaction sepayTx;

    @BeforeEach
    void setUp() {
        vnpayProps = new PaymentConfig.VNPayProperties();
        vnpayProps.setTmnCode("DEMOVNPAY");
        vnpayProps.setHashSecret("DEMOHASHSECRET1234567890ABCDEF");
        vnpayProps.setPayUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        vnpayProps.setReturnUrl("http://localhost:3000/checkout/payment-return");

        sepayProps = new PaymentConfig.SePayProperties();
        sepayProps.setApiKey("DEMO_SEPAY_API_KEY_123456");
        sepayProps.setAccountNumber("0123456789");
        sepayProps.setBank("MBBank");
        sepayProps.setQrUrlTemplate("https://qr.sepay.vn/img?acc={acc}&bank={bank}&amount={amount}&des={des}");

        User user = User.builder().id(1L).email("user@example.com").build();

        order = Order.builder()
                .id(100L)
                .user(user)
                .orderCode("EM-20260820-A1B2C")
                .status(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentStatus(PaymentStatus.UNPAID)
                .totalAmount(BigDecimal.valueOf(200000))
                .orderedAt(LocalDateTime.now())
                .build();

        vnpayTx = PaymentTransaction.builder()
                .id(1L)
                .order(order)
                .paymentRef("EM-20260820-A1B2C-P1A2B3C4D5E6")
                .gateway(PaymentGateway.VNPAY)
                .amount(BigDecimal.valueOf(200000))
                .status(PaymentTransactionStatus.PENDING)
                .build();

        sepayTx = PaymentTransaction.builder()
                .id(2L)
                .order(order)
                .paymentRef("EM-20260820-A1B2C-P6E5D4C3B2A1")
                .gateway(PaymentGateway.SEPAY)
                .amount(BigDecimal.valueOf(200000))
                .status(PaymentTransactionStatus.PENDING)
                .build();
    }

    // =========================================================================
    // VNPAY TESTS
    // =========================================================================

    @Test
    @DisplayName("Tạo VNPay Payment URL thành công chứa đầy đủ tham số và chữ ký HMAC-SHA512")
    void createVNPayPaymentUrl_Success() {
        when(paymentConfig.getVnpay()).thenReturn(vnpayProps);

        String paymentUrl = paymentService.createVNPayPaymentUrl(order, "EM-20260820-A1B2C-P1A2B3C4D5E6", "127.0.0.1");

        assertThat(paymentUrl).isNotEmpty();
        assertThat(paymentUrl).startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?");
        assertThat(paymentUrl).contains("vnp_TmnCode=DEMOVNPAY");
        assertThat(paymentUrl).contains("vnp_Amount=20000000"); // 200,000 * 100
        assertThat(paymentUrl).contains("vnp_TxnRef=EM-20260820-A1B2C-P1A2B3C4D5E6");
        assertThat(paymentUrl).contains("vnp_SecureHash=");
    }

    @Test
    @DisplayName("VNPay IPN thành công: xác thực chữ ký, cập nhật Order PAID và Tx SUCCESS")
    void processVNPayIpn_Success_WhenValidHashAndResponse00() {
        when(paymentConfig.getVnpay()).thenReturn(vnpayProps);
        when(paymentTransactionRepository.findByPaymentRef("EM-20260820-A1B2C-P1A2B3C4D5E6")).thenReturn(Optional.of(vnpayTx));

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_TmnCode", "DEMOVNPAY");
        vnpParams.put("vnp_Amount", "20000000");
        vnpParams.put("vnp_TxnRef", "EM-20260820-A1B2C-P1A2B3C4D5E6");
        vnpParams.put("vnp_ResponseCode", "00");
        vnpParams.put("vnp_TransactionNo", "14567890");

        String secureHash = VNPayUtil.hashAllFields(vnpParams, vnpayProps.getHashSecret());
        vnpParams.put("vnp_SecureHash", secureHash);

        VNPayIpnResponse response = paymentService.processVNPayIpn(vnpParams);

        assertThat(response.getRspCode()).isEqualTo("00");
        assertThat(response.getMessage()).isEqualTo("Confirm Success");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getPaidAt()).isNotNull();
        assertThat(vnpayTx.getStatus()).isEqualTo(PaymentTransactionStatus.SUCCESS);
        assertThat(vnpayTx.getGatewayTransactionNo()).isEqualTo("14567890");
        verify(orderRepository).save(order);
        verify(paymentTransactionRepository).save(vnpayTx);
    }

    @Test
    @DisplayName("VNPay IPN thất bại khi chữ ký SecureHash không hợp lệ -> trả về RspCode 97")
    void processVNPayIpn_InvalidChecksum_Returns97() {
        when(paymentConfig.getVnpay()).thenReturn(vnpayProps);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_TmnCode", "DEMOVNPAY");
        vnpParams.put("vnp_Amount", "20000000");
        vnpParams.put("vnp_TxnRef", "EM-20260820-A1B2C-P1A2B3C4D5E6");
        vnpParams.put("vnp_SecureHash", "INVALID_HASH_VALUE");

        VNPayIpnResponse response = paymentService.processVNPayIpn(vnpParams);

        assertThat(response.getRspCode()).isEqualTo("97");
        assertThat(response.getMessage()).isEqualTo("Invalid Checksum");
    }

    @Test
    @DisplayName("VNPay IPN thất bại khi vnp_TmnCode không khớp cấu hình -> trả về RspCode 97")
    void processVNPayIpn_InvalidTmnCode_Returns97() {
        when(paymentConfig.getVnpay()).thenReturn(vnpayProps);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_TmnCode", "WRONG_MERCHANT_CODE");
        vnpParams.put("vnp_Amount", "20000000");
        vnpParams.put("vnp_TxnRef", "EM-20260820-A1B2C-P1A2B3C4D5E6");

        String secureHash = VNPayUtil.hashAllFields(vnpParams, vnpayProps.getHashSecret());
        vnpParams.put("vnp_SecureHash", secureHash);

        VNPayIpnResponse response = paymentService.processVNPayIpn(vnpParams);

        assertThat(response.getRspCode()).isEqualTo("97");
    }

    @Test
    @DisplayName("VNPay IPN không tìm thấy paymentRef -> trả ngay RspCode 01, không tự tạo transaction")
    void processVNPayIpn_TransactionNotFound_Returns01_NoFallbackCreate() {
        when(paymentConfig.getVnpay()).thenReturn(vnpayProps);
        when(paymentTransactionRepository.findByPaymentRef("UNKNOWN_REF")).thenReturn(Optional.empty());

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_TmnCode", "DEMOVNPAY");
        vnpParams.put("vnp_Amount", "20000000");
        vnpParams.put("vnp_TxnRef", "UNKNOWN_REF");

        String secureHash = VNPayUtil.hashAllFields(vnpParams, vnpayProps.getHashSecret());
        vnpParams.put("vnp_SecureHash", secureHash);

        VNPayIpnResponse response = paymentService.processVNPayIpn(vnpParams);

        assertThat(response.getRspCode()).isEqualTo("01");
        assertThat(response.getMessage()).isEqualTo("Order not Found");
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("VNPay IPN số tiền không khớp snapshot transaction -> trả về RspCode 04")
    void processVNPayIpn_InvalidAmountVsTransactionSnapshot_Returns04() {
        when(paymentConfig.getVnpay()).thenReturn(vnpayProps);
        when(paymentTransactionRepository.findByPaymentRef("EM-20260820-A1B2C-P1A2B3C4D5E6")).thenReturn(Optional.of(vnpayTx));

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_TmnCode", "DEMOVNPAY");
        vnpParams.put("vnp_Amount", "10000000"); // 100,000 != snapshot 200,000
        vnpParams.put("vnp_TxnRef", "EM-20260820-A1B2C-P1A2B3C4D5E6");

        String secureHash = VNPayUtil.hashAllFields(vnpParams, vnpayProps.getHashSecret());
        vnpParams.put("vnp_SecureHash", secureHash);

        VNPayIpnResponse response = paymentService.processVNPayIpn(vnpParams);

        assertThat(response.getRspCode()).isEqualTo("04");
        assertThat(response.getMessage()).isEqualTo("Invalid Amount");
    }

    @Test
    @DisplayName("VNPay IPN Idempotency: Đơn hàng đã PAID -> trả về RspCode 02 không xử lý lại")
    void processVNPayIpn_DuplicateCallback_DoesNotProcessAgain_Returns02() {
        order.setPaymentStatus(PaymentStatus.PAID);
        vnpayTx.setStatus(PaymentTransactionStatus.SUCCESS);

        when(paymentConfig.getVnpay()).thenReturn(vnpayProps);
        when(paymentTransactionRepository.findByPaymentRef("EM-20260820-A1B2C-P1A2B3C4D5E6")).thenReturn(Optional.of(vnpayTx));

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_TmnCode", "DEMOVNPAY");
        vnpParams.put("vnp_Amount", "20000000");
        vnpParams.put("vnp_TxnRef", "EM-20260820-A1B2C-P1A2B3C4D5E6");
        vnpParams.put("vnp_ResponseCode", "00");

        String secureHash = VNPayUtil.hashAllFields(vnpParams, vnpayProps.getHashSecret());
        vnpParams.put("vnp_SecureHash", secureHash);

        VNPayIpnResponse response = paymentService.processVNPayIpn(vnpParams);

        assertThat(response.getRspCode()).isEqualTo("02");
        assertThat(response.getMessage()).isEqualTo("Order already confirmed");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("VNPay IPN thanh toán thất bại (code != 00): Cập nhật Tx FAILED, Order UNPAID")
    void processVNPayIpn_FailedPayment_UpdatesStatusToFailed() {
        when(paymentConfig.getVnpay()).thenReturn(vnpayProps);
        when(paymentTransactionRepository.findByPaymentRef("EM-20260820-A1B2C-P1A2B3C4D5E6")).thenReturn(Optional.of(vnpayTx));

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_TmnCode", "DEMOVNPAY");
        vnpParams.put("vnp_Amount", "20000000");
        vnpParams.put("vnp_TxnRef", "EM-20260820-A1B2C-P1A2B3C4D5E6");
        vnpParams.put("vnp_ResponseCode", "24"); // Khách hàng hủy giao dịch
        vnpParams.put("vnp_TransactionNo", "14567891");

        String secureHash = VNPayUtil.hashAllFields(vnpParams, vnpayProps.getHashSecret());
        vnpParams.put("vnp_SecureHash", secureHash);

        VNPayIpnResponse response = paymentService.processVNPayIpn(vnpParams);

        assertThat(response.getRspCode()).isEqualTo("00");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(vnpayTx.getStatus()).isEqualTo(PaymentTransactionStatus.FAILED);
        verify(orderRepository).save(order);
        verify(paymentTransactionRepository).save(vnpayTx);
    }

    @Test
    @DisplayName("VNPay Return: chỉ trả kết quả hiển thị cho UI, tuyệt đối KHÔNG mutate Order state")
    void processVNPayReturn_Success_DoesNotMutateOrderState() {
        when(paymentConfig.getVnpay()).thenReturn(vnpayProps);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_TmnCode", "DEMOVNPAY");
        vnpParams.put("vnp_Amount", "20000000");
        vnpParams.put("vnp_TxnRef", "EM-20260820-A1B2C-P1A2B3C4D5E6");
        vnpParams.put("vnp_ResponseCode", "00");
        vnpParams.put("vnp_TransactionNo", "14567890");

        String secureHash = VNPayUtil.hashAllFields(vnpParams, vnpayProps.getHashSecret());
        vnpParams.put("vnp_SecureHash", secureHash);

        VNPayReturnResponse response = paymentService.processVNPayReturn(vnpParams);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getOrderCode()).isEqualTo("EM-20260820-A1B2C");
        assertThat(response.getAmount()).isEqualByComparingTo("200000");
        verifyNoInteractions(orderRepository);
    }

    // =========================================================================
    // SEPAY TESTS
    // =========================================================================

    @Test
    @DisplayName("Tạo SePay VietQR URL thành công chứa đúng thông tin chuyển khoản và mã hóa url des")
    void createSePayQrUrl_Success() {
        when(paymentConfig.getSepay()).thenReturn(sepayProps);

        String qrUrl = paymentService.createSePayQrUrl(order, "EM-20260820-A1B2C-P6E5D4C3B2A1");

        assertThat(qrUrl).isNotEmpty();
        assertThat(qrUrl).contains("acc=0123456789");
        assertThat(qrUrl).contains("bank=MBBank");
        assertThat(qrUrl).contains("amount=200000");
        assertThat(qrUrl).contains("des=EM-20260820-A1B2C");
    }

    @Test
    @DisplayName("SePay Webhook thành công: lọc đúng PENDING SEPAY transaction, cập nhật Order PAID")
    void processSePayWebhook_Success_FiltersSepayPendingTransaction() {
        when(paymentConfig.getSepay()).thenReturn(sepayProps);
        when(orderRepository.findByOrderCode("EM-20260820-A1B2C")).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findFirstByOrderIdAndGatewayAndStatusOrderByCreatedAtDesc(
                100L, PaymentGateway.SEPAY, PaymentTransactionStatus.PENDING
        )).thenReturn(Optional.of(sepayTx));

        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .referenceCode("REF_998877")
                .accountNumber("0123456789")
                .transferType("in")
                .content("EM-20260820-A1B2C chuyen khoan")
                .transferAmount(BigDecimal.valueOf(200000))
                .build();

        paymentService.processSePayWebhook("Apikey DEMO_SEPAY_API_KEY_123456", request);

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getPaidAt()).isNotNull();
        assertThat(sepayTx.getStatus()).isEqualTo(PaymentTransactionStatus.SUCCESS);
        assertThat(sepayTx.getGatewayTransactionNo()).isEqualTo("REF_998877");
        verify(orderRepository).save(order);
        verify(paymentTransactionRepository).save(sepayTx);
    }

    @Test
    @DisplayName("SePay Webhook ném 401 Unauthorized khi thiếu header Authorization")
    void processSePayWebhook_MissingAuthHeader_ThrowsUnauthorized() {
        when(paymentConfig.getSepay()).thenReturn(sepayProps);
        SePayWebhookRequest request = SePayWebhookRequest.builder().build();

        assertThatThrownBy(() -> paymentService.processSePayWebhook(null, request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("API Key");
    }

    @Test
    @DisplayName("SePay Webhook ném 401 Unauthorized khi API Key không đúng")
    void processSePayWebhook_InvalidApiKey_ThrowsUnauthorized() {
        when(paymentConfig.getSepay()).thenReturn(sepayProps);
        SePayWebhookRequest request = SePayWebhookRequest.builder().build();

        assertThatThrownBy(() -> paymentService.processSePayWebhook("Apikey WRONG_KEY", request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("API Key");
    }

    @Test
    @DisplayName("SePay Webhook ném 422 khi referenceCode bị null hoặc rỗng")
    void processSePayWebhook_MissingReferenceCode_Returns422() {
        when(paymentConfig.getSepay()).thenReturn(sepayProps);
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .referenceCode("")
                .build();

        assertThatThrownBy(() -> paymentService.processSePayWebhook("Apikey DEMO_SEPAY_API_KEY_123456", request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("referenceCode");
    }

    @Test
    @DisplayName("SePay Webhook ném 422 khi số tài khoản nhận tiền không khớp")
    void processSePayWebhook_InvalidAccountNumber_Throws422() {
        when(paymentConfig.getSepay()).thenReturn(sepayProps);
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .referenceCode("REF_123")
                .accountNumber("9999999999") // Khác 0123456789
                .build();

        assertThatThrownBy(() -> paymentService.processSePayWebhook("Apikey DEMO_SEPAY_API_KEY_123456", request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Số tài khoản");
    }

    @Test
    @DisplayName("SePay Webhook ném 422 khi loại giao dịch không phải 'in'")
    void processSePayWebhook_Throws422_WhenInvalidTransferType() {
        when(paymentConfig.getSepay()).thenReturn(sepayProps);
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .referenceCode("REF_123")
                .accountNumber("0123456789")
                .transferType("out")
                .build();

        assertThatThrownBy(() -> paymentService.processSePayWebhook("Apikey DEMO_SEPAY_API_KEY_123456", request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Loại giao dịch");
    }

    @Test
    @DisplayName("SePay Webhook ném 422 khi không tìm thấy orderCode trong nội dung chuyển khoản")
    void processSePayWebhook_Throws422_WhenOrderCodeNotFoundInContent() {
        when(paymentConfig.getSepay()).thenReturn(sepayProps);
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .referenceCode("REF_123")
                .accountNumber("0123456789")
                .transferType("in")
                .content("Khong co ma don hang")
                .build();

        assertThatThrownBy(() -> paymentService.processSePayWebhook("Apikey DEMO_SEPAY_API_KEY_123456", request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Không tìm thấy mã đơn hàng");
    }

    @Test
    @DisplayName("SePay Webhook ném 422 khi content và description chứa 2 orderCode khác nhau")
    void processSePayWebhook_ConflictingOrderCodes_Returns422() {
        when(paymentConfig.getSepay()).thenReturn(sepayProps);
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .referenceCode("REF_123")
                .accountNumber("0123456789")
                .transferType("in")
                .content("EM-20260820-AAAAA")
                .description("EM-20260820-BBBBB")
                .build();

        assertThatThrownBy(() -> paymentService.processSePayWebhook("Apikey DEMO_SEPAY_API_KEY_123456", request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("không đồng nhất");
    }

    @Test
    @DisplayName("SePay Webhook ném 404 khi không tìm thấy Order trong cơ sở dữ liệu")
    void processSePayWebhook_ThrowsNotFound_WhenOrderNotFound() {
        when(paymentConfig.getSepay()).thenReturn(sepayProps);
        when(orderRepository.findByOrderCode("EM-20260820-A1B2C")).thenReturn(Optional.empty());

        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .referenceCode("REF_123")
                .accountNumber("0123456789")
                .transferType("in")
                .content("EM-20260820-A1B2C")
                .build();

        assertThatThrownBy(() -> paymentService.processSePayWebhook("Apikey DEMO_SEPAY_API_KEY_123456", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Đơn hàng không tồn tại");
    }

    @Test
    @DisplayName("SePay Webhook ném 422 khi không có PENDING SePay transaction nào, không tự tạo transaction")
    void processSePayWebhook_NoPendingTransactionFound_WhenOrderUnpaid_Throws422() {
        when(paymentConfig.getSepay()).thenReturn(sepayProps);
        when(orderRepository.findByOrderCode("EM-20260820-A1B2C")).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findFirstByOrderIdAndGatewayAndStatusOrderByCreatedAtDesc(
                100L, PaymentGateway.SEPAY, PaymentTransactionStatus.PENDING
        )).thenReturn(Optional.empty());

        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .referenceCode("REF_123")
                .accountNumber("0123456789")
                .transferType("in")
                .content("EM-20260820-A1B2C")
                .build();

        assertThatThrownBy(() -> paymentService.processSePayWebhook("Apikey DEMO_SEPAY_API_KEY_123456", request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Không tìm thấy giao dịch thanh toán SePay đang chờ xử lý");
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("SePay Webhook ném 422 khi chuyển thiếu tiền so với snapshot transaction")
    void processSePayWebhook_Throws422_WhenInsufficientAmount() {
        when(paymentConfig.getSepay()).thenReturn(sepayProps);
        when(orderRepository.findByOrderCode("EM-20260820-A1B2C")).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findFirstByOrderIdAndGatewayAndStatusOrderByCreatedAtDesc(
                100L, PaymentGateway.SEPAY, PaymentTransactionStatus.PENDING
        )).thenReturn(Optional.of(sepayTx));

        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .referenceCode("REF_123")
                .accountNumber("0123456789")
                .transferType("in")
                .content("EM-20260820-A1B2C")
                .transferAmount(BigDecimal.valueOf(150000)) // Yêu cầu 200,000
                .build();

        assertThatThrownBy(() -> paymentService.processSePayWebhook("Apikey DEMO_SEPAY_API_KEY_123456", request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("không đủ");
    }

    @Test
    @DisplayName("SePay Webhook ném 422 khi chuyển thừa tiền so với snapshot transaction")
    void processSePayWebhook_Throws422_WhenOverpayment() {
        when(paymentConfig.getSepay()).thenReturn(sepayProps);
        when(orderRepository.findByOrderCode("EM-20260820-A1B2C")).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findFirstByOrderIdAndGatewayAndStatusOrderByCreatedAtDesc(
                100L, PaymentGateway.SEPAY, PaymentTransactionStatus.PENDING
        )).thenReturn(Optional.of(sepayTx));

        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .referenceCode("REF_123")
                .accountNumber("0123456789")
                .transferType("in")
                .content("EM-20260820-A1B2C")
                .transferAmount(BigDecimal.valueOf(250000)) // Yêu cầu 200,000
                .build();

        assertThatThrownBy(() -> paymentService.processSePayWebhook("Apikey DEMO_SEPAY_API_KEY_123456", request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("vượt quá");
    }

    @Test
    @DisplayName("SePay Webhook Idempotency Step 5: Trùng referenceCode đã xử lý -> bỏ qua không làm gì")
    void processSePayWebhook_DuplicateReferenceCode_DoesNotCreateDuplicateTransaction() {
        when(paymentConfig.getSepay()).thenReturn(sepayProps);
        when(paymentTransactionRepository.existsByGatewayAndGatewayTransactionNo(PaymentGateway.SEPAY, "REF_DUP")).thenReturn(true);

        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .referenceCode("REF_DUP")
                .accountNumber("0123456789")
                .transferType("in")
                .build();

        paymentService.processSePayWebhook("Apikey DEMO_SEPAY_API_KEY_123456", request);

        verify(orderRepository, never()).save(any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("SePay Webhook Idempotency Step 8: Order đã PAID -> bỏ qua không làm gì")
    void processSePayWebhook_OrderAlreadyPaid_DoesNotCreateDuplicateTransaction() {
        order.setPaymentStatus(PaymentStatus.PAID);
        when(paymentConfig.getSepay()).thenReturn(sepayProps);
        when(orderRepository.findByOrderCode("EM-20260820-A1B2C")).thenReturn(Optional.of(order));

        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .referenceCode("REF_NEW")
                .accountNumber("0123456789")
                .transferType("in")
                .content("EM-20260820-A1B2C")
                .build();

        paymentService.processSePayWebhook("Apikey DEMO_SEPAY_API_KEY_123456", request);

        verify(orderRepository, never()).save(any());
        verify(paymentTransactionRepository, never()).save(any());
    }
}
