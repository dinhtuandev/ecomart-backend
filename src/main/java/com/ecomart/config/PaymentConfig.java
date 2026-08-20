package com.ecomart.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.payment")
@Getter
@Setter
public class PaymentConfig {

    private VNPayProperties vnpay = new VNPayProperties();
    private SePayProperties sepay = new SePayProperties();

    @Getter
    @Setter
    public static class VNPayProperties {
        private String tmnCode = "DEMOVNPAY";
        private String hashSecret = "DEMOHASHSECRET1234567890ABCDEF";
        private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        private String returnUrl = "http://localhost:3000/checkout/payment-return";
    }

    @Getter
    @Setter
    public static class SePayProperties {
        private String apiKey = "DEMO_SEPAY_API_KEY_123456";
        private String accountNumber = "0123456789";
        private String bank = "MBBank";
        private String qrUrlTemplate = "https://qr.sepay.vn/img?acc={acc}&bank={bank}&amount={amount}&des={des}";
    }
}
