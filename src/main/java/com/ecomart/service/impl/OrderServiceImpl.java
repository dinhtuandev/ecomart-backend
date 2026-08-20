package com.ecomart.service.impl;

import com.ecomart.dto.request.AdminCancelOrderRequest;
import com.ecomart.dto.request.CreateOrderRequest;
import com.ecomart.dto.request.UpdatePaymentStatusRequest;
import com.ecomart.dto.response.*;
import com.ecomart.entity.*;
import com.ecomart.entity.enums.*;
import com.ecomart.exception.ConflictException;
import com.ecomart.exception.ForbiddenException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.exception.UnprocessableEntityException;
import com.ecomart.repository.*;
import com.ecomart.service.OrderService;
import com.ecomart.service.PaymentService;
import com.ecomart.specification.OrderSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentService paymentService;

    @Override
    @Transactional
    public CreateOrderResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + userId));

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ giao hàng không tồn tại với ID: " + request.getAddressId()));

        if (!address.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Địa chỉ giao hàng không thuộc quyền sở hữu của bạn");
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new UnprocessableEntityException("Giỏ hàng đang trống, không thể đặt hàng"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new UnprocessableEntityException("Giỏ hàng đang trống, không thể đặt hàng");
        }

        // Validate stock and visibility for all items before placing order
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (!product.isVisible()) {
                throw new UnprocessableEntityException("Sản phẩm '" + product.getName() + "' tạm thời không khả dụng để đặt hàng");
            }

            Inventory inventory = inventoryRepository.findByProductId(product.getId())
                    .orElseThrow(() -> new UnprocessableEntityException("Không tìm thấy thông tin tồn kho của sản phẩm: " + product.getName()));

            if (inventory.getQuantity() < cartItem.getQuantity()) {
                throw new UnprocessableEntityException("Sản phẩm '" + product.getName() + "' không đủ tồn kho (còn " + inventory.getQuantity() + ", yêu cầu " + cartItem.getQuantity() + ")");
            }

            // Decrement inventory stock
            inventory.setQuantity(inventory.getQuantity() - cartItem.getQuantity());
            inventoryRepository.save(inventory);

            BigDecimal unitPrice = product.getSellingPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(unitPrice)
                    .quantity(cartItem.getQuantity())
                    .lineTotal(lineTotal)
                    .build();

            orderItems.add(orderItem);
        }

        String deliveryAddress = formatDeliveryAddress(address);
        String orderCode = generateOrderCode();

        Order order = Order.builder()
                .user(user)
                .orderCode(orderCode)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.UNPAID)
                .totalAmount(totalAmount)
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .deliveryAddress(deliveryAddress)
                .orderedAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
            orderItemRepository.save(item);
        }
        savedOrder.setItems(orderItems);

        // Clear cart
        cartItemRepository.deleteAllByCartId(cart.getId());
        cart.getItems().clear();

        // Handle online payment gateway transaction URL
        String paymentUrl = null;
        if (request.getPaymentMethod() != PaymentMethod.COD) {
            String paymentRef = generatePaymentRef(orderCode);
            PaymentGateway gateway = request.getPaymentMethod() == PaymentMethod.VNPAY ? PaymentGateway.VNPAY : PaymentGateway.SEPAY;

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .order(savedOrder)
                    .paymentRef(paymentRef)
                    .gateway(gateway)
                    .amount(totalAmount)
                    .status(PaymentTransactionStatus.PENDING)
                    .build();
            PaymentTransaction savedTx = paymentTransactionRepository.save(transaction);
            savedOrder.getPaymentTransactions().add(savedTx);

            if (gateway == PaymentGateway.VNPAY) {
                paymentUrl = paymentService.createVNPayPaymentUrl(savedOrder, paymentRef, null);
            } else {
                paymentUrl = paymentService.createSePayQrUrl(savedOrder, paymentRef);
            }
        }

        return CreateOrderResponse.builder()
                .order(mapToOrderResponse(savedOrder))
                .paymentUrl(paymentUrl)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getCustomerOrders(Long userId, OrderStatus status, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by(Sort.Direction.DESC, "orderedAt"));
        Page<Order> orderPage;

        if (status != null) {
            orderPage = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            orderPage = orderRepository.findByUserId(userId, pageable);
        }

        List<OrderResponse> items = orderPage.getContent().stream()
                .map(this::mapToOrderResponse)
                .toList();

        return PageResponse.from(items, orderPage);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getCustomerOrderDetail(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại với ID: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền truy cập đơn hàng này");
        }

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelCustomerOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại với ID: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền thao tác trên đơn hàng này");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("Chỉ có thể hủy đơn hàng khi đang ở trạng thái Chờ xác nhận (PENDING)");
        }

        // Restore inventory stock
        restoreInventoryStock(order);

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason("Khách hàng hủy đơn");
        order.setCancelledAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public CreateOrderResponse retryPayment(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại với ID: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền thao tác trên đơn hàng này");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("Chỉ có thể thanh toán lại cho đơn hàng đang Chờ xác nhận (PENDING)");
        }

        if (order.getPaymentMethod() == PaymentMethod.COD) {
            throw new ConflictException("Đơn hàng thanh toán COD không thể thực hiện thanh toán trực tuyến");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ConflictException("Đơn hàng này đã được thanh toán thành công");
        }

        String paymentRef = generatePaymentRef(order.getOrderCode());
        PaymentGateway gateway = order.getPaymentMethod() == PaymentMethod.VNPAY ? PaymentGateway.VNPAY : PaymentGateway.SEPAY;

        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .paymentRef(paymentRef)
                .gateway(gateway)
                .amount(order.getTotalAmount())
                .status(PaymentTransactionStatus.PENDING)
                .build();
        PaymentTransaction savedTx = paymentTransactionRepository.save(transaction);
        order.getPaymentTransactions().add(savedTx);

        String paymentUrl;
        if (gateway == PaymentGateway.VNPAY) {
            paymentUrl = paymentService.createVNPayPaymentUrl(order, paymentRef, null);
        } else {
            paymentUrl = paymentService.createSePayQrUrl(order, paymentRef);
        }

        return CreateOrderResponse.builder()
                .order(mapToOrderResponse(order))
                .paymentUrl(paymentUrl)
                .build();
    }

    // ==========================================
    // ADMIN OPERATIONS
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAdminOrders(
            int page,
            int pageSize,
            String keyword,
            OrderStatus status,
            PaymentStatus paymentStatus,
            PaymentMethod paymentMethod,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by(Sort.Direction.DESC, "orderedAt"));
        Specification<Order> spec = OrderSpecification.filterAdminOrders(keyword, status, paymentStatus, paymentMethod, fromDate, toDate);

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        List<OrderResponse> items = orderPage.getContent().stream()
                .map(this::mapToOrderResponse)
                .toList();

        return PageResponse.from(items, orderPage);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getAdminOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại với ID: " + orderId));
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại với ID: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("Chỉ có thể xác nhận đơn hàng đang ở trạng thái Chờ xác nhận (PENDING)");
        }

        if (order.getPaymentMethod() != PaymentMethod.COD && order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new ConflictException("Đơn hàng thanh toán online chưa thanh toán (PAID), không thể xác nhận");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse cancelAdminOrder(Long orderId, AdminCancelOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại với ID: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ConflictException("Không thể hủy đơn hàng ở trạng thái: " + order.getStatus());
        }

        // Restore inventory stock
        restoreInventoryStock(order);

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(request.getCancellationReason());
        order.setCancelledAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại với ID: " + orderId));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ConflictException("Chỉ có thể hoàn thành đơn hàng đang ở trạng thái Đã xác nhận (CONFIRMED)");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

        if (order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
        }

        Order savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updatePaymentStatus(Long orderId, UpdatePaymentStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại với ID: " + orderId));

        if (request.getPaymentStatus() != PaymentStatus.REFUNDED) {
            throw new ConflictException("Chỉ hỗ trợ cập nhật trạng thái thanh toán thành REFUNDED");
        }

        if (order.getStatus() != OrderStatus.CANCELLED) {
            throw new ConflictException("Chỉ có thể hoàn tiền cho đơn hàng đã Hủy (CANCELLED)");
        }

        if (order.getPaymentMethod() == PaymentMethod.COD) {
            throw new ConflictException("Không áp dụng hoàn tiền hệ thống cho đơn hàng COD");
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new ConflictException("Chỉ có thể hoàn tiền cho đơn hàng đã thanh toán trước đó (PAID)");
        }

        order.setPaymentStatus(PaymentStatus.REFUNDED);
        Order savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder);
    }

    private void restoreInventoryStock(Order order) {
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                inventoryRepository.findByProductId(item.getProduct().getId())
                        .ifPresent(inventory -> {
                            inventory.setQuantity(inventory.getQuantity() + item.getQuantity());
                            inventoryRepository.save(inventory);
                        });
            }
        }
    }

    private String generateOrderCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return "EM-" + datePart + "-" + randomPart;
    }

    private String generatePaymentRef(String orderCode) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return orderCode + "-P" + suffix;
    }

    private String formatDeliveryAddress(Address address) {
        return address.getAddressDetail() + ", " + address.getWard() + ", " + address.getDistrict() + ", " + address.getProvince();
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                String primaryImageUrl = getPrimaryImageUrl(item.getProduct());
                itemResponses.add(OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProductName())
                        .productImageUrl(primaryImageUrl)
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .lineTotal(item.getLineTotal())
                        .build());
            }
        }

        List<PaymentTransactionResponse> txResponses = new ArrayList<>();
        if (order.getPaymentTransactions() != null) {
            for (PaymentTransaction tx : order.getPaymentTransactions()) {
                txResponses.add(PaymentTransactionResponse.builder()
                        .id(tx.getId())
                        .paymentRef(tx.getPaymentRef())
                        .gateway(tx.getGateway() != null ? tx.getGateway().name() : null)
                        .amount(tx.getAmount())
                        .gatewayTransactionNo(tx.getGatewayTransactionNo())
                        .status(tx.getStatus() != null ? tx.getStatus().name() : null)
                        .createdAt(tx.getCreatedAt())
                        .build());
            }
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .totalAmount(order.getTotalAmount())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .cancellationReason(order.getCancellationReason())
                .orderedAt(order.getOrderedAt())
                .confirmedAt(order.getConfirmedAt())
                .completedAt(order.getCompletedAt())
                .cancelledAt(order.getCancelledAt())
                .paidAt(order.getPaidAt())
                .items(itemResponses)
                .paymentTransactions(txResponses)
                .build();
    }

    private String getPrimaryImageUrl(Product product) {
        if (product == null || product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        return product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElseGet(() -> product.getImages().get(0).getImageUrl());
    }
}
