package com.ecomart.controller;

import com.ecomart.dto.request.CreateOrderRequest;
import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.CreateOrderResponse;
import com.ecomart.dto.response.OrderResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.entity.enums.OrderStatus;
import com.ecomart.security.UserPrincipal;
import com.ecomart.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        CreateOrderResponse response = orderService.createOrder(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đặt hàng thành công.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getCustomerOrders(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageResponse<OrderResponse> response = orderService.getCustomerOrders(currentUser.getId(), status, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công.", response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getCustomerOrderDetail(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId
    ) {
        OrderResponse response = orderService.getCustomerOrderDetail(currentUser.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn hàng thành công.", response));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelCustomerOrder(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId
    ) {
        OrderResponse response = orderService.cancelCustomerOrder(currentUser.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công.", response));
    }

    @PostMapping("/{orderId}/retry-payment")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> retryPayment(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId
    ) {
        CreateOrderResponse response = orderService.retryPayment(currentUser.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Tạo lại giao dịch thanh toán thành công.", response));
    }
}
