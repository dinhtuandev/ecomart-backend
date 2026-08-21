package com.ecomart.service.impl;

import com.ecomart.dto.request.*;
import com.ecomart.dto.response.AuthResponse;
import com.ecomart.dto.response.ForgotPasswordResponse;
import com.ecomart.dto.response.RegisterResponse;
import com.ecomart.dto.response.UserResponse;
import com.ecomart.entity.EmailVerificationToken;
import com.ecomart.entity.PasswordResetToken;
import com.ecomart.entity.Role;
import com.ecomart.entity.User;
import com.ecomart.exception.*;
import com.ecomart.repository.EmailVerificationTokenRepository;
import com.ecomart.repository.PasswordResetTokenRepository;
import com.ecomart.repository.RoleRepository;
import com.ecomart.repository.UserRepository;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.security.UserPrincipal;
import com.ecomart.service.AuthService;
import com.ecomart.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int COOLDOWN_SECONDS = 60;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 15;
    private static final int MAX_REQUESTS_PER_WINDOW = 5;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email đã được sử dụng.");
        }

        // Kiểm tra Cooldown 60s & Rate Limit 5/15p trước khi gửi mail
        checkEmailVerificationCooldownAndRateLimit(normalizedEmail);

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Role CUSTOMER không tồn tại trên hệ thống."));

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .isActive(true)
                .isEmailVerified(false)
                .role(customerRole)
                .build();

        userRepository.save(user);

        // Tạo mã OTP xác thực email (6 chữ số, hiệu lực 5 phút)
        String otpCode = generateOtpCode();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .email(normalizedEmail)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .failedAttempts(0)
                .build();
        emailVerificationTokenRepository.save(verificationToken);

        // Gửi email bất đồng bộ
        emailService.sendVerificationOtp(normalizedEmail, user.getFullName(), otpCode);

        return RegisterResponse.builder()
                .message("Đăng ký tài khoản thành công. Vui lòng kiểm tra email để lấy mã OTP kích hoạt tài khoản.")
                .email(normalizedEmail)
                .otpCode(otpCode)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        EmailVerificationToken token = emailVerificationTokenRepository
                .findFirstByEmailAndIsUsedFalseOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy mã OTP kích hoạt nào đang hoạt động. Vui lòng yêu cầu gửi lại mã mới."));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại mã mới.");
        }

        // Chống Brute-force: Kiểm tra mã OTP
        if (!token.getOtpCode().equals(request.getOtpCode().trim())) {
            token.setFailedAttempts(token.getFailedAttempts() + 1);
            if (token.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                token.setUsed(true);
                emailVerificationTokenRepository.save(token);
                throw new BadRequestException("Bạn đã nhập sai mã OTP quá " + MAX_FAILED_ATTEMPTS + " lần. Mã kích hoạt này đã bị vô hiệu hóa, vui lòng yêu cầu gửi lại mã mới.");
            }
            emailVerificationTokenRepository.save(token);
            int remaining = MAX_FAILED_ATTEMPTS - token.getFailedAttempts();
            throw new BadRequestException("Mã OTP không chính xác. Bạn còn " + remaining + " lần thử.");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại."));

        user.setEmailVerified(true);
        userRepository.save(user);

        token.setUsed(true);
        emailVerificationTokenRepository.save(token);

        String accessToken = tokenProvider.generateToken(user.getEmail(), user.getId(), user.getRole().getName());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail(), user.getId(), user.getRole().getName());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getJwtExpirationMs())
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public void resendVerificationOtp(ResendOtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản với email này không tồn tại."));

        if (user.isEmailVerified()) {
            throw new BadRequestException("Tài khoản này đã được xác thực email trước đó.");
        }

        // Kiểm tra Cooldown 60s & Rate Limit 5/15p
        checkEmailVerificationCooldownAndRateLimit(normalizedEmail);

        // Hủy các OTP cũ chưa sử dụng
        List<EmailVerificationToken> oldTokens = emailVerificationTokenRepository.findByEmailAndIsUsedFalse(normalizedEmail);
        for (EmailVerificationToken oldToken : oldTokens) {
            oldToken.setUsed(true);
        }
        emailVerificationTokenRepository.saveAll(oldTokens);

        // Sinh OTP mới
        String otpCode = generateOtpCode();
        EmailVerificationToken newToken = EmailVerificationToken.builder()
                .email(normalizedEmail)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .failedAttempts(0)
                .build();
        emailVerificationTokenRepository.save(newToken);

        emailService.sendVerificationOtp(normalizedEmail, user.getFullName(), otpCode);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Email hoặc mật khẩu không chính xác."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Email hoặc mật khẩu không chính xác.");
        }

        if (!user.isActive()) {
            throw new ForbiddenException("Tài khoản của bạn đã bị khóa.");
        }

        if (!user.isEmailVerified()) {
            throw new UnauthorizedException("Tài khoản chưa được xác thực email. Vui lòng kích hoạt tài khoản bằng mã OTP đã gửi về email.");
        }

        String accessToken = tokenProvider.generateToken(user.getEmail(), user.getId(), user.getRole().getName());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail(), user.getId(), user.getRole().getName());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getJwtExpirationMs())
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!tokenProvider.validateToken(token)) {
            throw new UnauthorizedException("Refresh token không hợp lệ hoặc đã hết hạn.");
        }

        String tokenType = tokenProvider.getTokenType(token);
        if (!"REFRESH".equalsIgnoreCase(tokenType)) {
            throw new UnauthorizedException("Token được cung cấp không phải là Refresh token.");
        }

        String email = tokenProvider.getEmailFromToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Người dùng không tồn tại."));

        if (!user.isActive()) {
            throw new ForbiddenException("Tài khoản của bạn đã bị khóa.");
        }

        if (!user.isEmailVerified()) {
            throw new UnauthorizedException("Tài khoản chưa được xác thực email.");
        }

        String newAccessToken = tokenProvider.generateToken(user.getEmail(), user.getId(), user.getRole().getName());
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getEmail(), user.getId(), user.getRole().getName());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getJwtExpirationMs())
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản với email này không tồn tại."));

        // Kiểm tra Cooldown 60s & Rate Limit 5/15p
        checkPasswordResetCooldownAndRateLimit(user);

        // Hủy các token reset mật khẩu cũ chưa dùng
        List<PasswordResetToken> oldTokens = passwordResetTokenRepository.findByUserAndIsUsedFalse(user);
        for (PasswordResetToken oldToken : oldTokens) {
            oldToken.setUsed(true);
        }
        passwordResetTokenRepository.saveAll(oldTokens);

        String resetOtp = generateOtpCode();
        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .user(user)
                .token(resetOtp)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .isUsed(false)
                .failedAttempts(0)
                .build();

        passwordResetTokenRepository.save(passwordResetToken);

        emailService.sendPasswordResetOtp(normalizedEmail, user.getFullName(), resetOtp);

        return ForgotPasswordResponse.builder()
                .message("Yêu cầu đặt lại mật khẩu đã được xử lý. Vui lòng kiểm tra mã OTP gửi về email.")
                .resetToken(resetOtp)
                .build();
    }

    @Override
    @Transactional
    public void resetPasswordWithOtp(ResetPasswordWithOtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại."));

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findFirstByUserAndIsUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy mã OTP đặt lại mật khẩu nào đang hoạt động. Vui lòng yêu cầu gửi lại mã mới."));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Mã OTP đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu gửi lại mã mới.");
        }

        // Chống Brute-force: Kiểm tra mã OTP
        if (!resetToken.getToken().equals(request.getOtpCode().trim())) {
            resetToken.setFailedAttempts(resetToken.getFailedAttempts() + 1);
            if (resetToken.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                resetToken.setUsed(true);
                passwordResetTokenRepository.save(resetToken);
                throw new BadRequestException("Bạn đã nhập sai mã OTP quá " + MAX_FAILED_ATTEMPTS + " lần. Mã đặt lại mật khẩu này đã bị vô hiệu hóa, vui lòng yêu cầu gửi lại mã mới.");
            }
            passwordResetTokenRepository.save(resetToken);
            int remaining = MAX_FAILED_ATTEMPTS - resetToken.getFailedAttempts();
            throw new BadRequestException("Mã OTP không chính xác. Bạn còn " + remaining + " lần thử.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Token đặt lại mật khẩu không hợp lệ."));

        if (resetToken.isUsed()) {
            throw new BadRequestException("Token đặt lại mật khẩu đã được sử dụng.");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token đặt lại mật khẩu đã hết hạn.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để tiếp tục.");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại."));

        if (!user.isActive()) {
            throw new ForbiddenException("Tài khoản của bạn đã bị khóa.");
        }

        return mapToUserResponse(user);
    }

    private void checkEmailVerificationCooldownAndRateLimit(String email) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Kiểm tra Cooldown 60 giây
        Optional<EmailVerificationToken> latestTokenOpt = emailVerificationTokenRepository.findFirstByEmailOrderByCreatedAtDesc(email);
        if (latestTokenOpt.isPresent()) {
            LocalDateTime createdAt = latestTokenOpt.get().getCreatedAt();
            if (createdAt != null && createdAt.isAfter(now.minusSeconds(COOLDOWN_SECONDS))) {
                long elapsedSeconds = Duration.between(createdAt, now).getSeconds();
                long waitSeconds = Math.max(1, COOLDOWN_SECONDS - elapsedSeconds);
                throw new TooManyRequestsException("Vui lòng đợi " + waitSeconds + " giây trước khi yêu cầu mã OTP kích hoạt tiếp theo.");
            }
        }

        // 2. Kiểm tra Rate Limit (Tối đa 5 lần trong 15 phút)
        long recentRequestsCount = emailVerificationTokenRepository.countByEmailAndCreatedAtAfter(email, now.minusMinutes(RATE_LIMIT_WINDOW_MINUTES));
        if (recentRequestsCount >= MAX_REQUESTS_PER_WINDOW) {
            throw new TooManyRequestsException("Bạn đã vượt quá giới hạn yêu cầu OTP kích hoạt (tối đa " + MAX_REQUESTS_PER_WINDOW + " lần trong " + RATE_LIMIT_WINDOW_MINUTES + " phút). Vui lòng thử lại sau.");
        }
    }

    private void checkPasswordResetCooldownAndRateLimit(User user) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Kiểm tra Cooldown 60 giây
        Optional<PasswordResetToken> latestTokenOpt = passwordResetTokenRepository.findFirstByUserOrderByCreatedAtDesc(user);
        if (latestTokenOpt.isPresent()) {
            LocalDateTime createdAt = latestTokenOpt.get().getCreatedAt();
            if (createdAt != null && createdAt.isAfter(now.minusSeconds(COOLDOWN_SECONDS))) {
                long elapsedSeconds = Duration.between(createdAt, now).getSeconds();
                long waitSeconds = Math.max(1, COOLDOWN_SECONDS - elapsedSeconds);
                throw new TooManyRequestsException("Vui lòng đợi " + waitSeconds + " giây trước khi yêu cầu mã OTP đặt lại mật khẩu tiếp theo.");
            }
        }

        // 2. Kiểm tra Rate Limit (Tối đa 5 lần trong 15 phút)
        long recentRequestsCount = passwordResetTokenRepository.countByUserAndCreatedAtAfter(user, now.minusMinutes(RATE_LIMIT_WINDOW_MINUTES));
        if (recentRequestsCount >= MAX_REQUESTS_PER_WINDOW) {
            throw new TooManyRequestsException("Bạn đã vượt quá giới hạn yêu cầu OTP đặt lại mật khẩu (tối đa " + MAX_REQUESTS_PER_WINDOW + " lần trong " + RATE_LIMIT_WINDOW_MINUTES + " phút). Vui lòng thử lại sau.");
        }
    }

    private String generateOtpCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.isActive())
                .isEmailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
