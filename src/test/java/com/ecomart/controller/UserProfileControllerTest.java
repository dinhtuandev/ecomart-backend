package com.ecomart.controller;

import com.ecomart.dto.request.ChangePasswordRequest;
import com.ecomart.dto.request.UpdateProfileRequest;
import com.ecomart.dto.response.UserResponse;
import com.ecomart.exception.BadRequestException;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.UserPrincipal;
import com.ecomart.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserPrincipal userPrincipal;
    private UsernamePasswordAuthenticationToken authToken;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.create(10L, "customer@example.com", "pass", "CUSTOMER", true);
        authToken = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        userResponse = UserResponse.builder()
                .id(10L)
                .fullName("Nguyen Van A")
                .email("customer@example.com")
                .phoneNumber("0901234567")
                .role("CUSTOMER")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/me trả về thông tin hồ sơ")
    void getProfile_Success() throws Exception {
        when(userService.getUserProfile(10L)).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/me")
                        .with(authentication(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("customer@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Nguyen Van A"));
    }

    @Test
    @DisplayName("PATCH /api/v1/me cập nhật thông tin thành công")
    void updateProfile_Success() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyen Van B")
                .phoneNumber("0988888888")
                .build();

        UserResponse updatedResponse = UserResponse.builder()
                .id(10L)
                .fullName("Nguyen Van B")
                .email("customer@example.com")
                .phoneNumber("0988888888")
                .role("CUSTOMER")
                .isActive(true)
                .build();

        when(userService.updateUserProfile(eq(10L), any(UpdateProfileRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(patch("/api/v1/me")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Nguyen Van B"));
    }

    @Test
    @DisplayName("PATCH /api/v1/me/password thành công khi dữ liệu hợp lệ")
    void changePassword_Success() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword123")
                .newPassword("newPassword123")
                .build();

        doNothing().when(userService).changePassword(eq(10L), any(ChangePasswordRequest.class));

        mockMvc.perform(patch("/api/v1/me/password")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công."));
    }
}
