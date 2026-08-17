package com.ecomart.controller;

import com.ecomart.dto.request.UpdateUserStatusRequest;
import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.dto.response.UserResponse;
import com.ecomart.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by("id").descending());
        PageResponse<UserResponse> response = userService.getUsersForAdmin(keyword, isActive, pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khách hàng thành công.", response));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        UserResponse response = userService.updateUserStatusForAdmin(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái khách hàng thành công.", response));
    }
}
