package com.ecomart.service;

import com.ecomart.dto.request.ForgotPasswordRequest;
import com.ecomart.dto.request.LoginRequest;
import com.ecomart.dto.request.RegisterRequest;
import com.ecomart.dto.request.ResetPasswordRequest;
import com.ecomart.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
