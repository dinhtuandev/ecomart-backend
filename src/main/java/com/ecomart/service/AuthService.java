package com.ecomart.service;

import com.ecomart.dto.request.*;
import com.ecomart.dto.response.AuthResponse;
import com.ecomart.dto.response.ForgotPasswordResponse;
import com.ecomart.dto.response.RegisterResponse;
import com.ecomart.dto.response.UserResponse;
import com.ecomart.security.UserPrincipal;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    AuthResponse verifyEmail(VerifyEmailRequest request);

    void resendVerificationOtp(ResendOtpRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);

    void resetPasswordWithOtp(ResetPasswordWithOtpRequest request);

    void resetPassword(ResetPasswordRequest request);

    UserResponse getCurrentUser(UserPrincipal principal);
}
