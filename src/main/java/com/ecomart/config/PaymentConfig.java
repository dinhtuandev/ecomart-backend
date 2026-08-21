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
        private String tmnCode;
        private String hashSecret;
        private String payUrl;
        private String returnUrl;
    }

    @Getter
    @Setter
    public static class SePayProperties {
        private String apiKey;
        private String accountNumber;
        private String bank;
        private String qrUrlTemplate;
    }
}
