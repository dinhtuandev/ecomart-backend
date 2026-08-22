package com.ecomart.controller;

import com.ecomart.dto.request.SePayWebhookRequest;
import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.VNPayIpnResponse;
import com.ecomart.dto.response.VNPayReturnResponse;
import com.ecomart.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @RequestMapping(value = "/vnpay/ipn", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<VNPayIpnResponse> handleVNPayIpn(@RequestParam Map<String, String> vnpParams) {
        VNPayIpnResponse response = paymentService.processVNPayIpn(vnpParams);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<ApiResponse<VNPayReturnResponse>> handleVNPayReturn(@RequestParam Map<String, String> vnpParams) {
        VNPayReturnResponse response = paymentService.processVNPayReturn(vnpParams);
        return ResponseEntity.ok(ApiResponse.success("Xử lý kết quả giao diện VNPay thành công.", response));
    }

    @PostMapping("/sepay/webhook")
    public ResponseEntity<ApiResponse<Void>> handleSePayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Api-Key", required = false) String xApiKey,
            @RequestHeader(value = "x-api-key", required = false) String xApiKeyLower,
            @RequestBody SePayWebhookRequest request
    ) {
        String effectiveAuth = authHeader;
        if ((effectiveAuth == null || effectiveAuth.isBlank()) && xApiKey != null) {
            effectiveAuth = xApiKey;
        } else if ((effectiveAuth == null || effectiveAuth.isBlank()) && xApiKeyLower != null) {
            effectiveAuth = xApiKeyLower;
        }
        try {
            paymentService.processSePayWebhook(effectiveAuth, request);
        } catch (DataIntegrityViolationException ex) {
            // Idempotent graceful fallback for concurrent database duplicate constraint collision
            return ResponseEntity.ok(ApiResponse.success("Giao dịch đã được xử lý trước đó.", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Xử lý thanh toán SePay thành công.", null));
    }
}
