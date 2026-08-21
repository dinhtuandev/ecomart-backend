package com.ecomart.service;

import com.ecomart.dto.request.*;
import com.ecomart.dto.response.AuthResponse;
import com.ecomart.dto.response.ForgotPasswordResponse;
import com.ecomart.dto.response.UserResponse;
import com.ecomart.entity.PasswordResetToken;
import com.ecomart.entity.Role;
import com.ecomart.entity.User;
import com.ecomart.exception.BadRequestException;
import com.ecomart.exception.ConflictException;
import com.ecomart.exception.ForbiddenException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.exception.UnauthorizedException;
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
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

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
                .role(customerRole)
                .build();
    }

    @Test
    @DisplayName("Register thành công khi email chưa tồn tại")
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
        when(tokenProvider.generateToken(anyString(), anyLong(), anyString())).thenReturn("mockJwtToken");
        when(tokenProvider.generateRefreshToken(anyString(), anyLong(), anyString())).thenReturn("mockRefreshToken");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mockJwtToken");
        assertThat(response.getRefreshToken()).isEqualTo("mockRefreshToken");
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        verify(userRepository, times(1)).save(any(User.class));
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
    @DisplayName("Login thành công khi email và password đúng")
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
        assertThat(response.getUser().getRole()).isEqualTo("CUSTOMER");
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
    @DisplayName("Login thất bại khi tài khoản bị khóa (isActive = false)")
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
    @DisplayName("Refresh token thành công khi refresh token hợp lệ")
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
        assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
    }

    @Test
    @DisplayName("Refresh token thất bại khi tài khoản bị khóa")
    void refreshToken_ThrowsForbidden_WhenAccountInactive() {
        testUser.setActive(false);
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        when(tokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(tokenProvider.getTokenType("valid-refresh-token")).thenReturn("REFRESH");
        when(tokenProvider.getEmailFromToken("valid-refresh-token")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Tài khoản của bạn đã bị khóa");
    }

    @Test
    @DisplayName("Lấy thông tin tài khoản hiện tại getCurrentUser thành công")
    void getCurrentUser_Success() {
        UserPrincipal principal = UserPrincipal.create(10L, "test@example.com", "pass", "CUSTOMER", true);
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));

        UserResponse response = authService.getCurrentUser(principal);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFullName()).isEqualTo("Nguyễn Văn A");
    }

    @Test
    @DisplayName("Forgot password thành công khi email tồn tại")
    void forgotPassword_Success() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("test@example.com")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        ForgotPasswordResponse response = authService.forgotPassword(request);

        assertThat(response).isNotNull();
        assertThat(response.getResetToken()).isNotBlank();
        verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("Forgot password thất bại khi email không tồn tại")
    void forgotPassword_EmailNotFound_ThrowsException() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("notfound@example.com")
                .build();

        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.forgotPassword(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tài khoản với email này không tồn tại");
    }

    @Test
    @DisplayName("Reset password thành công khi token hợp lệ")
    void resetPassword_Success() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-uuid-token")
                .newPassword("MatKhauMoi123")
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .token("valid-uuid-token")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .isUsed(false)
                .build();

        when(passwordResetTokenRepository.findByToken("valid-uuid-token")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("MatKhauMoi123")).thenReturn("newEncodedPassword");

        authService.resetPassword(request);

        assertThat(testUser.getPasswordHash()).isEqualTo("newEncodedPassword");
        assertThat(resetToken.isUsed()).isTrue();
        verify(userRepository, times(1)).save(testUser);
        verify(passwordResetTokenRepository, times(1)).save(resetToken);
    }

    @Test
    @DisplayName("Reset password thất bại khi token không tồn tại")
    void resetPassword_InvalidToken_ThrowsException() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("invalid-token")
                .newPassword("MatKhauMoi123")
                .build();

        when(passwordResetTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Token đặt lại mật khẩu không hợp lệ");
    }

    @Test
    @DisplayName("Reset password thất bại khi token đã hết hạn")
    void resetPassword_ExpiredToken_ThrowsException() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("expired-token")
                .newPassword("MatKhauMoi123")
                .build();

        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .id(1L)
                .token("expired-token")
                .user(testUser)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .isUsed(false)
                .build();

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Token đặt lại mật khẩu đã hết hạn");
    }
}
