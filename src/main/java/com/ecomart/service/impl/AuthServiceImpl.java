package com.ecomart.service.impl;

import com.ecomart.dto.request.*;
import com.ecomart.dto.response.AuthResponse;
import com.ecomart.dto.response.ForgotPasswordResponse;
import com.ecomart.dto.response.UserResponse;
import com.ecomart.entity.PasswordResetToken;
import com.ecomart.entity.Role;
import com.ecomart.entity.User;
import com.ecomart.exception.*;
import com.ecomart.repository.PasswordResetTokenRepository;
import com.ecomart.repository.RoleRepository;
import com.ecomart.repository.UserRepository;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.security.UserPrincipal;
import com.ecomart.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email đã được sử dụng.");
        }

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Role CUSTOMER không tồn tại trên hệ thống."));

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .isActive(true)
                .role(customerRole)
                .build();

        User savedUser = userRepository.save(user);

        String accessToken = tokenProvider.generateToken(savedUser.getEmail(), savedUser.getId(), savedUser.getRole().getName());
        String refreshToken = tokenProvider.generateRefreshToken(savedUser.getEmail(), savedUser.getId(), savedUser.getRole().getName());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getJwtExpirationMs())
                .user(mapToUserResponse(savedUser))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Email hoặc mật khẩu không chính xác."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Email hoặc mật khẩu không chính xác.");
        }

        if (!user.isActive()) {
            throw new ForbiddenException("Tài khoản của bạn đã bị khóa.");
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
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản với email này không tồn tại."));

        String resetToken = UUID.randomUUID().toString();
        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .user(user)
                .token(resetToken)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .isUsed(false)
                .build();

        passwordResetTokenRepository.save(passwordResetToken);

        return ForgotPasswordResponse.builder()
                .message("Yêu cầu đặt lại mật khẩu đã được xử lý. Vui lòng sử dụng mã xác nhận để đặt lại mật khẩu.")
                .resetToken(resetToken)
                .build();
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

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
