package com.ecomart.service;

import com.ecomart.dto.request.*;
import com.ecomart.dto.response.AuthResponse;
import com.ecomart.dto.response.ForgotPasswordResponse;
import com.ecomart.dto.response.UserResponse;
import com.ecomart.security.UserPrincipal;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    UserResponse getCurrentUser(UserPrincipal principal);
}
