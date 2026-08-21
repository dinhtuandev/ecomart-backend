package com.ecomart.service;

import com.ecomart.dto.request.*;
import com.ecomart.dto.response.*;
import com.ecomart.entity.EmailVerificationToken;
import com.ecomart.entity.PasswordResetToken;
import com.ecomart.entity.Role;
import com.ecomart.entity.User;
import com.ecomart.exception.BadRequestException;
import com.ecomart.exception.ConflictException;
import com.ecomart.exception.ForbiddenException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.exception.UnauthorizedException;
import com.ecomart.repository.EmailVerificationTokenRepository;
import com.ecomart.repository.PasswordResetTokenRepository;
import com.ecomart.repository.RoleRepository;
import com.ecomart.repository.UserRepository;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.security.UserPrincipal;
import com.ecomart.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role customerRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        customerRole = Role.builder()
                .id(1L)
                .name("CUSTOMER")
                .description("Customer Role")
                .build();

        testUser = User.builder()
                .id(10L)
                .fullName("Nguyễn Văn A")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .phoneNumber("0901234567")
                .isActive(true)
                .isEmailVerified(true)
                .role(customerRole)
                .build();
    }

    @Test
    @DisplayName("Register thành công sinh mã OTP và gửi email kích hoạt")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .email("test@example.com")
                .password("MatKhau123")
                .phoneNumber("0901234567")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("MatKhau123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        RegisterResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getOtpCode()).hasSize(6);
        verify(emailVerificationTokenRepository, times(1)).save(any(EmailVerificationToken.class));
        verify(emailService, times(1)).sendVerificationOtp(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("Register ném ConflictException 409 khi email đã tồn tại")
    void register_EmailAlreadyExists_ThrowsConflictException() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .email("test@example.com")
                .password("MatKhau123")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email đã được sử dụng");
    }

    @Test
    @DisplayName("Verify email thành công với OTP hợp lệ và cấp bộ token")
    void verifyEmail_Success() {
        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .email("test@example.com")
                .otpCode("123456")
                .build();

        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(1L)
                .email("test@example.com")
                .otpCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .isUsed(false)
                .build();

        when(emailVerificationTokenRepository.findByEmailAndOtpCodeAndIsUsedFalse("test@example.com", "123456"))
                .thenReturn(Optional.of(token));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateToken("test@example.com", 10L, "CUSTOMER")).thenReturn("mockJwtToken");
        when(tokenProvider.generateRefreshToken("test@example.com", 10L, "CUSTOMER")).thenReturn("mockRefreshToken");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.verifyEmail(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mockJwtToken");
        assertThat(token.isUsed()).isTrue();
        assertThat(testUser.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("Verify email thất bại khi OTP hết hạn")
    void verifyEmail_ThrowsBadRequest_WhenOtpExpired() {
        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .email("test@example.com")
                .otpCode("123456")
                .build();

        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(1L)
                .email("test@example.com")
                .otpCode("123456")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .isUsed(false)
                .build();

        when(emailVerificationTokenRepository.findByEmailAndOtpCodeAndIsUsedFalse("test@example.com", "123456"))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mã OTP đã hết hạn");
    }

    @Test
    @DisplayName("Gửi lại OTP kích hoạt tài khoản thành công")
    void resendVerificationOtp_Success() {
        testUser.setEmailVerified(false);
        ResendOtpRequest request = ResendOtpRequest.builder().email("test@example.com").build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(emailVerificationTokenRepository.findByEmailAndIsUsedFalse("test@example.com")).thenReturn(List.of());

        authService.resendVerificationOtp(request);

        verify(emailVerificationTokenRepository, times(1)).save(any(EmailVerificationToken.class));
        verify(emailService, times(1)).sendVerificationOtp(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("Login thành công khi email, password đúng và tài khoản đã verify email")
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("MatKhau123")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("MatKhau123", "encodedPassword")).thenReturn(true);
        when(tokenProvider.generateToken("test@example.com", 10L, "CUSTOMER")).thenReturn("mockJwtToken");
        when(tokenProvider.generateRefreshToken("test@example.com", 10L, "CUSTOMER")).thenReturn("mockRefreshToken");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mockJwtToken");
        assertThat(response.getRefreshToken()).isEqualTo("mockRefreshToken");
    }

    @Test
    @DisplayName("Login thất bại khi tài khoản chưa kích hoạt email")
    void login_UnverifiedEmail_ThrowsUnauthorizedException() {
        testUser.setEmailVerified(false);
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("MatKhau123")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("MatKhau123", "encodedPassword")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Tài khoản chưa được xác thực email");
    }

    @Test
    @DisplayName("Login thất bại khi sai password")
    void login_WrongPassword_ThrowsUnauthorizedException() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("WrongPassword")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Email hoặc mật khẩu không chính xác");
    }

    @Test
    @DisplayName("Login thất bại khi tài khoản bị khóa")
    void login_AccountInactive_ThrowsForbiddenException() {
        testUser.setActive(false);
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("MatKhau123")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("MatKhau123", "encodedPassword")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Tài khoản của bạn đã bị khóa");
    }

    @Test
    @DisplayName("Refresh token thành công")
    void refreshToken_Success() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        when(tokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(tokenProvider.getTokenType("valid-refresh-token")).thenReturn("REFRESH");
        when(tokenProvider.getEmailFromToken("valid-refresh-token")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateToken("test@example.com", 10L, "CUSTOMER")).thenReturn("newAccessToken");
        when(tokenProvider.generateRefreshToken("test@example.com", 10L, "CUSTOMER")).thenReturn("newRefreshToken");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
    }

    @Test
    @DisplayName("Lấy thông tin tài khoản hiện tại getCurrentUser thành công")
    void getCurrentUser_Success() {
        UserPrincipal principal = UserPrincipal.create(10L, "test@example.com", "pass", "CUSTOMER", true);
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));

        UserResponse response = authService.getCurrentUser(principal);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("Forgot password thành công sinh mã OTP gửi email")
    void forgotPassword_Success() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("test@example.com")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        ForgotPasswordResponse response = authService.forgotPassword(request);

        assertThat(response).isNotNull();
        assertThat(response.getResetToken()).hasSize(6);
        verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendPasswordResetOtp(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("Reset password với OTP thành công")
    void resetPasswordWithOtp_Success() {
        ResetPasswordWithOtpRequest request = ResetPasswordWithOtpRequest.builder()
                .email("test@example.com")
                .otpCode("654321")
                .newPassword("MatKhauMoi123")
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .token("654321")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .isUsed(false)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.findByToken("654321")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("MatKhauMoi123")).thenReturn("newEncodedPassword");

        authService.resetPasswordWithOtp(request);

        assertThat(testUser.getPasswordHash()).isEqualTo("newEncodedPassword");
        assertThat(resetToken.isUsed()).isTrue();
    }
}
