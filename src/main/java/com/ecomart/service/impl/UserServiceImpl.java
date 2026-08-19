package com.ecomart.service.impl;

import com.ecomart.dto.request.ChangePasswordRequest;
import com.ecomart.dto.request.UpdateProfileRequest;
import com.ecomart.dto.request.UpdateUserStatusRequest;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.dto.response.UserResponse;
import com.ecomart.entity.User;
import com.ecomart.exception.BadRequestException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.UserRepository;
import com.ecomart.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Long userId) {
        User user = findUserById(userId);
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu hiện tại không chính xác");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsersForAdmin(String keyword, Boolean isActive, Pageable pageable) {
        Page<User> userPage = userRepository.findCustomersForAdmin("CUSTOMER", keyword, isActive, pageable);
        List<UserResponse> responses = userPage.getContent().stream()
                .map(this::mapToUserResponse)
                .toList();
        return PageResponse.from(responses, userPage);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatusForAdmin(Long userId, UpdateUserStatusRequest request) {
        User user = findUserById(userId);

        if ("ADMIN".equalsIgnoreCase(user.getRole().getName())) {
            throw new BadRequestException("Không được phép thay đổi trạng thái của tài khoản Admin");
        }

        user.setActive(request.getIsActive());
        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + userId));
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().getName())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
