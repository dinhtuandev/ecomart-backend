package com.ecomart.controller;

import com.ecomart.dto.request.AdminCancelOrderRequest;
import com.ecomart.dto.response.OrderResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.entity.enums.OrderStatus;
import com.ecomart.entity.enums.PaymentMethod;
import com.ecomart.entity.enums.PaymentStatus;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.security.UserPrincipal;
import com.ecomart.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserPrincipal adminPrincipal;
    private UsernamePasswordAuthenticationToken authToken;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        adminPrincipal = UserPrincipal.create(99L, "admin@ecomart.com", "pass", "ADMIN", true);
        authToken = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        orderResponse = OrderResponse.builder()
                .id(1000L)
                .orderCode("EM-20260820-0001")
                .status(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.UNPAID)
                .totalAmount(BigDecimal.valueOf(400000))
                .recipientName("Nguyen Van A")
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/orders - Admin lấy danh sách đơn hàng trả về HTTP 200")
    void getAdminOrders_Returns200() throws Exception {
        PageResponse<OrderResponse> pageResponse = PageResponse.from(List.of(orderResponse), new PageImpl<>(List.of(orderResponse), PageRequest.of(0, 10), 1));

        when(orderService.getAdminOrders(anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/orders")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].orderCode").value("EM-20260820-0001"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/orders/{id}/confirm - Admin xác nhận đơn hàng trả về HTTP 200")
    void confirmOrder_Returns200() throws Exception {
        orderResponse.setStatus(OrderStatus.CONFIRMED);
        when(orderService.confirmOrder(1000L)).thenReturn(orderResponse);

        mockMvc.perform(post("/api/v1/admin/orders/1000/confirm")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/orders/{id}/cancel - Admin hủy đơn hàng trả về HTTP 200")
    void cancelAdminOrder_Returns200() throws Exception {
        AdminCancelOrderRequest request = AdminCancelOrderRequest.builder().cancellationReason("Lỗi kho").build();
        orderResponse.setStatus(OrderStatus.CANCELLED);
        orderResponse.setCancellationReason("Lỗi kho");

        when(orderService.cancelAdminOrder(eq(1000L), any(AdminCancelOrderRequest.class))).thenReturn(orderResponse);

        mockMvc.perform(post("/api/v1/admin/orders/1000/cancel")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
