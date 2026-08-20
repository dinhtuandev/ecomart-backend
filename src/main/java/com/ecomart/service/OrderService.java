package com.ecomart.service;

import com.ecomart.dto.request.AdminCancelOrderRequest;
import com.ecomart.dto.request.CreateOrderRequest;
import com.ecomart.dto.request.UpdatePaymentStatusRequest;
import com.ecomart.dto.response.CreateOrderResponse;
import com.ecomart.dto.response.OrderResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.entity.enums.OrderStatus;
import com.ecomart.entity.enums.PaymentMethod;
import com.ecomart.entity.enums.PaymentStatus;

import java.time.LocalDate;

public interface OrderService {

    // Customer Operations
    CreateOrderResponse createOrder(Long userId, CreateOrderRequest request);

    PageResponse<OrderResponse> getCustomerOrders(Long userId, OrderStatus status, int page, int pageSize);

    OrderResponse getCustomerOrderDetail(Long userId, Long orderId);

    OrderResponse cancelCustomerOrder(Long userId, Long orderId);

    CreateOrderResponse retryPayment(Long userId, Long orderId);

    // Admin Operations
    PageResponse<OrderResponse> getAdminOrders(
            int page,
            int pageSize,
            String keyword,
            OrderStatus status,
            PaymentStatus paymentStatus,
            PaymentMethod paymentMethod,
            LocalDate fromDate,
            LocalDate toDate
    );

    OrderResponse getAdminOrderDetail(Long orderId);

    OrderResponse confirmOrder(Long orderId);

    OrderResponse cancelAdminOrder(Long orderId, AdminCancelOrderRequest request);

    OrderResponse completeOrder(Long orderId);

    OrderResponse updatePaymentStatus(Long orderId, UpdatePaymentStatusRequest request);
}
