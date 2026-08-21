package com.ecomart.controller;

import com.ecomart.dto.request.*;
import com.ecomart.dto.response.*;
import com.ecomart.exception.UnauthorizedException;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private AuthResponse authResponse;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userResponse = UserResponse.builder()
                .id(1L)
                .fullName("Nguyễn Văn A")
                .email("test@example.com")
                .role("CUSTOMER")
                .phoneNumber("0901234567")
                .isActive(true)
                .isEmailVerified(true)
                .build();

        authResponse = AuthResponse.builder()
                .accessToken("test-jwt-token")
                .refreshToken("test-refresh-token")
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register trả về HTTP 201 kèm mã OTP")
    void register_Success_Returns201() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .email("test@example.com")
                .password("MatKhau123")
                .phoneNumber("0901234567")
                .build();

        RegisterResponse registerResponse = RegisterResponse.builder()
                .message("Đăng ký tài khoản thành công.")
                .email("test@example.com")
                .otpCode("123456")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(registerResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.otpCode").value("123456"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/verify-email trả về HTTP 200 kèm AuthResponse")
    void verifyEmail_Success_Returns200() throws Exception {
        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .email("test@example.com")
                .otpCode("123456")
                .build();

        when(authService.verifyEmail(any(VerifyEmailRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("test-jwt-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/resend-verification trả về HTTP 200")
    void resendVerificationOtp_Returns200() throws Exception {
        ResendOtpRequest request = ResendOtpRequest.builder()
                .email("test@example.com")
                .build();

        doNothing().when(authService).resendVerificationOtp(any(ResendOtpRequest.class));

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login trả về HTTP 200 khi đăng nhập thành công")
    void login_Success_Returns200() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("MatKhau123")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("test-jwt-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh-token trả về HTTP 200 khi cấp mới token thành công")
    void refreshToken_Success_Returns200() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("test-jwt-token"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me trả về HTTP 200 thông tin user hiện tại")
    void getCurrentUser_Returns200() throws Exception {
        when(authService.getCurrentUser(any())).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/auth/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.isEmailVerified").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password trả về HTTP 200 kèm resetToken")
    void forgotPassword_Returns200() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("test@example.com")
                .build();

        ForgotPasswordResponse response = ForgotPasswordResponse.builder()
                .message("Yêu cầu đặt lại mật khẩu đã được xử lý.")
                .resetToken("654321")
                .build();

        when(authService.forgotPassword(any(ForgotPasswordRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resetToken").value("654321"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password-otp trả về HTTP 200")
    void resetPasswordWithOtp_Returns200() throws Exception {
        ResetPasswordWithOtpRequest request = ResetPasswordWithOtpRequest.builder()
                .email("test@example.com")
                .otpCode("654321")
                .newPassword("MatKhauMoi123")
                .build();

        doNothing().when(authService).resetPasswordWithOtp(any(ResetPasswordWithOtpRequest.class));

        mockMvc.perform(post("/api/v1/auth/reset-password-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout trả về HTTP 200")
    void logout_Returns200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng xuất thành công."));
    }
}
