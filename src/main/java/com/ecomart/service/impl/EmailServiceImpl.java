package com.ecomart.service.impl;

import com.ecomart.service.EmailService;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    @Async
    @Override
    public void sendVerificationOtp(String toEmail, String fullName, String otpCode) {
        String subject = "[EcoMart] Mã xác thực kích hoạt tài khoản của bạn";
        String htmlContent = buildOtpEmailTemplate(
                fullName,
                "Cảm ơn bạn đã đăng ký tài khoản tại EcoMart - Sàn thương mại điện tử thân thiện môi trường!",
                "Mã OTP kích hoạt tài khoản của bạn là:",
                otpCode,
                "Mã này có hiệu lực trong vòng 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai."
        );

        sendEmailViaResend(toEmail, subject, htmlContent, otpCode, "VERIFICATION");
    }

    @Async
    @Override
    public void sendPasswordResetOtp(String toEmail, String fullName, String otpCode) {
        String subject = "[EcoMart] Mã OTP đặt lại mật khẩu";
        String htmlContent = buildOtpEmailTemplate(
                fullName,
                "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản EcoMart của bạn.",
                "Mã OTP đặt lại mật khẩu của bạn là:",
                otpCode,
                "Mã này có hiệu lực trong vòng 15 phút. Nếu bạn không gửi yêu cầu này, vui lòng bỏ qua email."
        );

        sendEmailViaResend(toEmail, subject, htmlContent, otpCode, "PASSWORD_RESET");
    }

    private void sendEmailViaResend(String toEmail, String subject, String htmlContent, String otpCode, String type) {
        if (resendApiKey == null || resendApiKey.isBlank() || resendApiKey.startsWith("re_xxxx")) {
            log.info("========== [RESEND SIMULATION] ==========");
            log.info("Type: {}", type);
            log.info("To: {}", toEmail);
            log.info("Subject: {}", subject);
            log.info("OTP Code: {}", otpCode);
            log.info("=========================================");
            return;
        }

        try {
            Resend resend = new Resend(resendApiKey);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(toEmail)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            log.info("Đã gửi email thành công qua Resend tới {} (Email ID: {})", toEmail, response != null ? response.getId() : "N/A");
        } catch (Exception e) {
            log.warn("Không thể gửi email thực tế qua Resend tới {} (Lỗi: {}). Sử dụng chế độ DEV fallback. OTP: {}",
                    toEmail, e.getMessage(), otpCode);
        }
    }

    private String buildOtpEmailTemplate(String name, String greeting, String instruction, String otpCode, String note) {
        return "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; background-color: #f9fafb; border-radius: 12px; border: 1px solid #e5e7eb;\">"
                + "<div style=\"text-align: center; margin-bottom: 24px;\">"
                + "<h1 style=\"color: #16a34a; font-size: 28px; margin: 0; font-weight: 800; letter-spacing: -0.5px;\">🌿 EcoMart</h1>"
                + "<p style=\"color: #4b5563; font-size: 14px; margin-top: 4px;\">Sống Xanh - Mua Sắm Bền Vững</p>"
                + "</div>"
                + "<div style=\"background-color: #ffffff; padding: 32px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);\">"
                + "<h2 style=\"color: #111827; font-size: 18px; margin-top: 0;\">Xin chào " + (name != null ? name : "Quý khách") + ",</h2>"
                + "<p style=\"color: #374151; font-size: 15px; line-height: 1.6;\">" + greeting + "</p>"
                + "<p style=\"color: #374151; font-size: 15px; margin-bottom: 8px;\">" + instruction + "</p>"
                + "<div style=\"text-align: center; margin: 28px 0;\">"
                + "<span style=\"display: inline-block; font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #15803d; background-color: #dcfce7; padding: 14px 28px; border-radius: 8px; border: 2px dashed #86efac;\">"
                + otpCode + "</span>"
                + "</div>"
                + "<p style=\"color: #6b7280; font-size: 13px; line-height: 1.5;\">" + note + "</p>"
                + "</div>"
                + "<div style=\"text-align: center; margin-top: 24px; color: #9ca3af; font-size: 12px;\">"
                + "<p>© 2026 EcoMart. Bảo vệ môi trường bắt đầu từ thói quen tiêu dùng của bạn.</p>"
                + "</div>"
                + "</div>";
    }
}
