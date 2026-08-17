package com.ecomart.controller;

import com.ecomart.dto.request.ChangePasswordRequest;
import com.ecomart.dto.request.UpdateProfileRequest;
import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.UserResponse;
import com.ecomart.security.UserPrincipal;
import com.ecomart.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        UserResponse response = userService.getUserProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hồ sơ thành công.", response));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@AuthenticationPrincipal UserPrincipal currentUser,
                                                                    @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse response = userService.updateUserProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin hồ sơ thành công.", response));
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal UserPrincipal currentUser,
                                                             @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công.", null));
    }
}
