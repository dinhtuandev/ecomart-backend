package com.ecomart.controller;

import com.ecomart.dto.request.SePayWebhookRequest;
import com.ecomart.dto.response.VNPayIpnResponse;
import com.ecomart.dto.response.VNPayReturnResponse;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST /api/v1/payments/vnpay/ipn - Tiếp nhận IPN callback trả về HTTP 200")
    void vnpayIpn_Returns200() throws Exception {
        VNPayIpnResponse ipnResponse = new VNPayIpnResponse("00", "Confirm Success");
        when(paymentService.processVNPayIpn(anyMap())).thenReturn(ipnResponse);

        mockMvc.perform(post("/api/v1/payments/vnpay/ipn")
                        .param("vnp_TmnCode", "DEMOVNPAY")
                        .param("vnp_Amount", "20000000")
                        .param("vnp_TxnRef", "EM-20260820-A1B2C-P1A2B3C4D5E6")
                        .param("vnp_ResponseCode", "00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/vnpay/return - Tiếp nhận Return redirect trả về HTTP 200")
    void vnpayReturn_Returns200() throws Exception {
        VNPayReturnResponse returnResponse = VNPayReturnResponse.builder()
                .orderCode("EM-20260820-A1B2C")
                .amount(BigDecimal.valueOf(200000))
                .isSuccess(true)
                .message("Thanh toán thành công")
                .build();

        when(paymentService.processVNPayReturn(anyMap())).thenReturn(returnResponse);

        mockMvc.perform(get("/api/v1/payments/vnpay/return")
                        .param("vnp_Amount", "20000000")
                        .param("vnp_TxnRef", "EM-20260820-A1B2C-P1A2B3C4D5E6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderCode").value("EM-20260820-A1B2C"))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/payments/sepay/webhook - Tiếp nhận Webhook thành công trả về HTTP 200")
    void sepayWebhook_Success_Returns200() throws Exception {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .referenceCode("REF_123456")
                .accountNumber("0123456789")
                .transferType("in")
                .content("EM-20260820-A1B2C")
                .transferAmount(BigDecimal.valueOf(200000))
                .build();

        doNothing().when(paymentService).processSePayWebhook(any(), any());

        mockMvc.perform(post("/api/v1/payments/sepay/webhook")
                        .header("Authorization", "Apikey DEMO_SEPAY_API_KEY_123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xử lý thanh toán SePay thành công."));
    }

    @Test
    @DisplayName("POST /api/v1/payments/sepay/webhook - Bắt DataIntegrityViolationException và trả về HTTP 200 Idempotent")
    void sepayWebhook_DataIntegrityViolationException_HandledGracefullyAs200() throws Exception {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .referenceCode("REF_DUPLICATE")
                .build();

        doThrow(new DataIntegrityViolationException("Duplicate key error"))
                .when(paymentService).processSePayWebhook(any(), any());

        mockMvc.perform(post("/api/v1/payments/sepay/webhook")
                        .header("Authorization", "Apikey DEMO_SEPAY_API_KEY_123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Giao dịch đã được xử lý trước đó."));
    }
}
