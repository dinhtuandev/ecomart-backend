package com.ecomart;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ResendIntegrationTest {

    @Test
    @Disabled("Chạy thủ công để kiểm tra gửi email thực tế qua Resend khi đã set RESEND_API_KEY")
    void sendTestEmail() throws Exception {
        String apiKey = System.getenv("RESEND_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }

        Resend resend = new Resend(apiKey);

        CreateEmailOptions sendEmailRequest = CreateEmailOptions.builder()
                .from("onboarding@resend.dev")
                .to("trandinhtuan0219@gmail.com")
                .subject("[EcoMart] Xin chào từ Sàn Thương Mại Điện Tử EcoMart!")
                .html("<div style=\"font-family: Arial, sans-serif; padding: 20px; background-color: #f0fdf4; border-radius: 8px;\">"
                        + "<h2 style=\"color: #16a34a;\">🌿 Chào mừng bạn đến với EcoMart!</h2>"
                        + "<p>Tích hợp Resend API thành công.</p>"
                        + "</div>")
                .build();

        CreateEmailResponse data = resend.emails().send(sendEmailRequest);
        System.out.println(">>> [RESEND SUCCESS] Email sent successfully! ID: " + data.getId());
    }
}
