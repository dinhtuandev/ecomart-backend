package com.ecomart.controller;

import com.ecomart.dto.request.AddToCartRequest;
import com.ecomart.dto.request.UpdateCartItemRequest;
import com.ecomart.dto.response.CartItemResponse;
import com.ecomart.dto.response.CartResponse;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.security.UserPrincipal;
import com.ecomart.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserPrincipal userPrincipal;
    private UsernamePasswordAuthenticationToken authToken;
    private CartResponse cartResponse;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.create(1L, "customer@example.com", "pass", "CUSTOMER", true);
        authToken = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        CartItemResponse itemResponse = CartItemResponse.builder()
                .id(1L)
                .productId(100L)
                .productName("Bình nước tre")
                .sellingPrice(BigDecimal.valueOf(150000))
                .quantity(2)
                .subtotal(BigDecimal.valueOf(300000))
                .quantityInStock(10)
                .isVisible(true)
                .isAvailable(true)
                .build();

        cartResponse = CartResponse.builder()
                .id(10L)
                .items(List.of(itemResponse))
                .totalItems(1)
                .totalQuantity(2)
                .totalAmount(BigDecimal.valueOf(300000))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/cart - Lấy giỏ hàng thành công trả về HTTP 200")
    void getCart_Success_Returns200() throws Exception {
        when(cartService.getCart(1L)).thenReturn(cartResponse);

        mockMvc.perform(get("/api/v1/cart")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].productName").value("Bình nước tre"))
                .andExpect(jsonPath("$.data.totalAmount").value(300000));
    }

    @Test
    @DisplayName("POST /api/v1/cart/items - Thêm sản phẩm vào giỏ hàng thành công trả về HTTP 200")
    void addToCart_Success_Returns200() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder().productId(100L).quantity(2).build();

        when(cartService.addToCart(eq(1L), any(AddToCartRequest.class))).thenReturn(cartResponse);

        mockMvc.perform(post("/api/v1/cart/items")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/cart/items - Dữ liệu không hợp lệ (quantity = 0) trả về HTTP 400")
    void addToCart_InvalidPayload_Returns400() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder().productId(100L).quantity(0).build();

        mockMvc.perform(post("/api/v1/cart/items")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PATCH /api/v1/cart/items/{cartItemId} - Cập nhật số lượng trả về HTTP 200")
    void updateCartItem_Success_Returns200() throws Exception {
        UpdateCartItemRequest request = UpdateCartItemRequest.builder().quantity(5).build();

        when(cartService.updateCartItem(eq(1L), eq(1L), any(UpdateCartItemRequest.class))).thenReturn(cartResponse);

        mockMvc.perform(patch("/api/v1/cart/items/1")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/v1/cart/items/{cartItemId} - Xóa sản phẩm khỏi giỏ trả về HTTP 200")
    void removeCartItem_Success_Returns200() throws Exception {
        doNothing().when(cartService).removeCartItem(1L, 1L);

        mockMvc.perform(delete("/api/v1/cart/items/1")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/v1/cart - Làm trống giỏ hàng trả về HTTP 200")
    void clearCart_Success_Returns200() throws Exception {
        doNothing().when(cartService).clearCart(1L);

        mockMvc.perform(delete("/api/v1/cart")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
