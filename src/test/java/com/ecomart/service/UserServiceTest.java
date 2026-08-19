package com.ecomart.service;

import com.ecomart.dto.request.ChangePasswordRequest;
import com.ecomart.dto.request.UpdateProfileRequest;
import com.ecomart.dto.request.UpdateUserStatusRequest;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.dto.response.UserResponse;
import com.ecomart.entity.Role;
import com.ecomart.entity.User;
import com.ecomart.exception.BadRequestException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.UserRepository;
import com.ecomart.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private Role customerRole;
    private Role adminRole;
    private User customerUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        customerRole = Role.builder().id(1L).name("CUSTOMER").description("Khách hàng").build();
        adminRole = Role.builder().id(2L).name("ADMIN").description("Quản trị viên").build();

        customerUser = User.builder()
                .id(10L)
                .fullName("Nguyen Van Customer")
                .email("customer@example.com")
                .passwordHash("encoded_password")
                .phoneNumber("0901234567")
                .isActive(true)
                .role(customerRole)
                .createdAt(LocalDateTime.now())
                .build();

        adminUser = User.builder()
                .id(99L)
                .fullName("System Admin")
                .email("admin@example.com")
                .passwordHash("encoded_admin_password")
                .phoneNumber("0909999999")
                .isActive(true)
                .role(adminRole)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getUserProfile - Thành công khi user tồn tại")
    void getUserProfile_Success() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(customerUser));

        UserResponse response = userService.getUserProfile(10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("customer@example.com");
        assertThat(response.getRole()).isEqualTo("CUSTOMER");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("getUserProfile - Ném ngoại lệ ResourceNotFoundException khi user không tồn tại")
    void getUserProfile_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Người dùng không tồn tại");
    }

    @Test
    @DisplayName("updateUserProfile - Thành công cập nhật họ tên và số điện thoại")
    void updateUserProfile_Success() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyen Van A Updated")
                .phoneNumber("0988888888")
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(customerUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUserProfile(10L, request);

        assertThat(response.getFullName()).isEqualTo("Nguyen Van A Updated");
        assertThat(response.getPhoneNumber()).isEqualTo("0988888888");
        verify(userRepository, times(1)).save(customerUser);
    }

    @Test
    @DisplayName("changePassword - Thành công đổi mật khẩu hợp lệ")
    void changePassword_Success() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old_password")
                .newPassword("new_password_123")
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(customerUser));
        when(passwordEncoder.matches("old_password", "encoded_password")).thenReturn(true);
        when(passwordEncoder.matches("new_password_123", "encoded_password")).thenReturn(false);
        when(passwordEncoder.encode("new_password_123")).thenReturn("encoded_new_password");

        userService.changePassword(10L, request);

        verify(passwordEncoder).encode("new_password_123");
        verify(userRepository).save(customerUser);
        assertThat(customerUser.getPasswordHash()).isEqualTo("encoded_new_password");
    }

    @Test
    @DisplayName("changePassword - Thất bại khi mật khẩu hiện tại sai")
    void changePassword_WrongCurrentPassword() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrong_password")
                .newPassword("new_password_123")
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(customerUser));
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mật khẩu hiện tại không chính xác");
    }

    @Test
    @DisplayName("changePassword - Thất bại khi mật khẩu mới trùng mật khẩu cũ")
    void changePassword_SamePassword() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old_password")
                .newPassword("old_password")
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(customerUser));
        when(passwordEncoder.matches("old_password", "encoded_password")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mật khẩu mới không được trùng với mật khẩu hiện tại");
    }

    @Test
    @DisplayName("getUsersForAdmin - Lấy danh sách khách hàng phân trang thành công")
    void getUsersForAdmin_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(Collections.singletonList(customerUser), pageable, 1);

        when(userRepository.findCustomersForAdmin(eq("CUSTOMER"), eq("Nguyen"), eq(true), eq(pageable)))
                .thenReturn(userPage);

        PageResponse<UserResponse> response = userService.getUsersForAdmin("Nguyen", true, pageable);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getPagination().getTotalItems()).isEqualTo(1);
        assertThat(response.getItems().get(0).getEmail()).isEqualTo("customer@example.com");
    }

    @Test
    @DisplayName("updateUserStatusForAdmin - Thành công cập nhật trạng thái của Customer")
    void updateUserStatusForAdmin_Success() {
        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder().isActive(false).build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(customerUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUserStatusForAdmin(10L, request);

        assertThat(response.isActive()).isFalse();
        verify(userRepository).save(customerUser);
    }

    @Test
    @DisplayName("updateUserStatusForAdmin - Thất bại khi cố gắng cập nhật trạng thái của Admin")
    void updateUserStatusForAdmin_TargetAdmin_ThrowsException() {
        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder().isActive(false).build();

        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> userService.updateUserStatusForAdmin(99L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không được phép thay đổi trạng thái của tài khoản Admin");
    }
}
