package com.ecomart.service;

import com.ecomart.dto.request.ChangePasswordRequest;
import com.ecomart.dto.request.UpdateProfileRequest;
import com.ecomart.dto.request.UpdateUserStatusRequest;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse getUserProfile(Long userId);

    UserResponse updateUserProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    PageResponse<UserResponse> getUsersForAdmin(String keyword, Boolean isActive, Pageable pageable);

    UserResponse updateUserStatusForAdmin(Long userId, UpdateUserStatusRequest request);
}
