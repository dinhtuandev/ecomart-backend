package com.ecomart.service;

public interface EmailService {

    void sendVerificationOtp(String toEmail, String fullName, String otpCode);

    void sendPasswordResetOtp(String toEmail, String fullName, String otpCode);
}
