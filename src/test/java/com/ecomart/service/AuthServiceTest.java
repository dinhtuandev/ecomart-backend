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
import com.ecomart.exception.TooManyRequestsException;
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
        when(emailVerificationTokenRepository.findFirstByEmailOrderByCreatedAtDesc("test@example.com"))
                .thenReturn(Optional.empty());
        when(emailVerificationTokenRepository.countByEmailAndCreatedAtAfter(eq("test@example.com"), any(LocalDateTime.class)))
                .thenReturn(0L);

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
    @DisplayName("Register ném TooManyRequestsException 429 khi vi phạm Cooldown 60s")
    void register_ThrowsTooManyRequests_WhenWithinCooldown() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .email("test@example.com")
                .password("MatKhau123")
                .build();

        EmailVerificationToken recentToken = EmailVerificationToken.builder()
                .email("test@example.com")
                .createdAt(LocalDateTime.now().minusSeconds(20))
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(emailVerificationTokenRepository.findFirstByEmailOrderByCreatedAtDesc("test@example.com"))
                .thenReturn(Optional.of(recentToken));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("Vui lòng đợi");
    }

    @Test
    @DisplayName("Register ném TooManyRequestsException 429 khi vượt quá Rate Limit 5 lần/15 phút")
    void register_ThrowsTooManyRequests_WhenExceedingRateLimit() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .email("test@example.com")
                .password("MatKhau123")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(emailVerificationTokenRepository.findFirstByEmailOrderByCreatedAtDesc("test@example.com"))
                .thenReturn(Optional.empty());
        when(emailVerificationTokenRepository.countByEmailAndCreatedAtAfter(eq("test@example.com"), any(LocalDateTime.class)))
                .thenReturn(5L);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("Bạn đã vượt quá giới hạn yêu cầu OTP");
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
                .failedAttempts(0)
                .build();

        when(emailVerificationTokenRepository.findFirstByEmailAndIsUsedFalseOrderByCreatedAtDesc("test@example.com"))
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
    @DisplayName("Verify email tăng failedAttempts khi sai OTP và ném BadRequestException")
    void verifyEmail_IncrementsFailedAttempts_WhenWrongOtp() {
        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .email("test@example.com")
                .otpCode("999999")
                .build();

        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(1L)
                .email("test@example.com")
                .otpCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .isUsed(false)
                .failedAttempts(1)
                .build();

        when(emailVerificationTokenRepository.findFirstByEmailAndIsUsedFalseOrderByCreatedAtDesc("test@example.com"))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mã OTP không chính xác. Bạn còn 3 lần thử.");

        assertThat(token.getFailedAttempts()).isEqualTo(2);
        assertThat(token.isUsed()).isFalse();
        verify(emailVerificationTokenRepository, times(1)).save(token);
    }

    @Test
    @DisplayName("Verify email vô hiệu hóa mã khi nhập sai quá 5 lần liên tiếp (Anti-Brute-Force)")
    void verifyEmail_DisablesToken_WhenExceeding5Attempts() {
        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .email("test@example.com")
                .otpCode("999999")
                .build();

        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(1L)
                .email("test@example.com")
                .otpCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .isUsed(false)
                .failedAttempts(4)
                .build();

        when(emailVerificationTokenRepository.findFirstByEmailAndIsUsedFalseOrderByCreatedAtDesc("test@example.com"))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Bạn đã nhập sai mã OTP quá 5 lần. Mã kích hoạt này đã bị vô hiệu hóa");

        assertThat(token.getFailedAttempts()).isEqualTo(5);
        assertThat(token.isUsed()).isTrue();
        verify(emailVerificationTokenRepository, times(1)).save(token);
    }

    @Test
    @DisplayName("Gửi lại OTP kích hoạt tài khoản thành công")
    void resendVerificationOtp_Success() {
        testUser.setEmailVerified(false);
        ResendOtpRequest request = ResendOtpRequest.builder().email("test@example.com").build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(emailVerificationTokenRepository.findFirstByEmailOrderByCreatedAtDesc("test@example.com"))
                .thenReturn(Optional.empty());
        when(emailVerificationTokenRepository.countByEmailAndCreatedAtAfter(eq("test@example.com"), any(LocalDateTime.class)))
                .thenReturn(0L);
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
    @DisplayName("Forgot password thành công sinh mã OTP gửi email")
    void forgotPassword_Success() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("test@example.com")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.findFirstByUserOrderByCreatedAtDesc(testUser))
                .thenReturn(Optional.empty());
        when(passwordResetTokenRepository.countByUserAndCreatedAtAfter(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(passwordResetTokenRepository.findByUserAndIsUsedFalse(testUser)).thenReturn(List.of());

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
                .failedAttempts(0)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.findFirstByUserAndIsUsedFalseOrderByCreatedAtDesc(testUser))
                .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("MatKhauMoi123")).thenReturn("newEncodedPassword");

        authService.resetPasswordWithOtp(request);

        assertThat(testUser.getPasswordHash()).isEqualTo("newEncodedPassword");
        assertThat(resetToken.isUsed()).isTrue();
    }
}
