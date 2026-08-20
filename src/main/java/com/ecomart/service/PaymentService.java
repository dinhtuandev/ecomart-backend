package com.ecomart.service;

import com.ecomart.dto.request.SePayWebhookRequest;
import com.ecomart.dto.response.VNPayIpnResponse;
import com.ecomart.dto.response.VNPayReturnResponse;
import com.ecomart.entity.Order;

import java.util.Map;

public interface PaymentService {

    String createVNPayPaymentUrl(Order order, String paymentRef, String ipAddress);

    String createSePayQrUrl(Order order, String paymentRef);

    VNPayIpnResponse processVNPayIpn(Map<String, String> vnpParams);

    VNPayReturnResponse processVNPayReturn(Map<String, String> vnpParams);

    void processSePayWebhook(String authHeader, SePayWebhookRequest request);
}
