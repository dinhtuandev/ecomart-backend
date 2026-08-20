package com.ecomart.controller;

import com.ecomart.dto.request.AdminCancelOrderRequest;
import com.ecomart.dto.request.UpdatePaymentStatusRequest;
import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.OrderResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.entity.enums.OrderStatus;
import com.ecomart.entity.enums.PaymentMethod;
import com.ecomart.entity.enums.PaymentStatus;
import com.ecomart.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAdminOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        PageResponse<OrderResponse> response = orderService.getAdminOrders(
                page, pageSize, keyword, status, paymentStatus, paymentMethod, fromDate, toDate
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng quản trị thành công.", response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getAdminOrderDetail(@PathVariable Long orderId) {
        OrderResponse response = orderService.getAdminOrderDetail(orderId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn hàng thành công.", response));
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(@PathVariable Long orderId) {
        OrderResponse response = orderService.confirmOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận đơn hàng thành công.", response));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelAdminOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody AdminCancelOrderRequest request
    ) {
        OrderResponse response = orderService.cancelAdminOrder(orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công.", response));
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> completeOrder(@PathVariable Long orderId) {
        OrderResponse response = orderService.completeOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success("Hoàn thành đơn hàng thành công.", response));
    }

    @PatchMapping("/{orderId}/payment-status")
    public ResponseEntity<ApiResponse<OrderResponse>> updatePaymentStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdatePaymentStatusRequest request
    ) {
        OrderResponse response = orderService.updatePaymentStatus(orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái hoàn tiền thành công.", response));
    }
}
