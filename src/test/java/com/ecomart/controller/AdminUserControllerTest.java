package com.ecomart.controller;

import com.ecomart.dto.request.UpdateUserStatusRequest;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.dto.response.UserResponse;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserPrincipal adminPrincipal;
    private UsernamePasswordAuthenticationToken authToken;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        adminPrincipal = UserPrincipal.create(99L, "admin@example.com", "pass", "ADMIN", true);
        authToken = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        userResponse = UserResponse.builder()
                .id(10L)
                .fullName("Nguyen Van Customer")
                .email("customer@example.com")
                .phoneNumber("0901234567")
                .role("CUSTOMER")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/users trả về danh sách phân trang khách hàng")
    void getUsers_Success() throws Exception {
        Pageable pageable = PageRequest.of(0, 12, Sort.by("id").descending());
        PageResponse<UserResponse> pageResponse = PageResponse.from(List.of(userResponse), new PageImpl<>(List.of(userResponse), pageable, 1));

        when(userService.getUsersForAdmin(eq("Nguyen"), eq(true), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("keyword", "Nguyen")
                        .param("isActive", "true")
                        .param("page", "1")
                        .param("pageSize", "12")
                        .with(authentication(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].email").value("customer@example.com"))
                .andExpect(jsonPath("$.data.pagination.totalItems").value(1));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{userId}/status cập nhật trạng thái người dùng")
    void updateUserStatus_Success() throws Exception {
        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder().isActive(false).build();
        UserResponse updatedResponse = UserResponse.builder()
                .id(10L)
                .fullName("Nguyen Van Customer")
                .email("customer@example.com")
                .role("CUSTOMER")
                .isActive(false)
                .build();

        when(userService.updateUserStatusForAdmin(eq(10L), any(UpdateUserStatusRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(patch("/api/v1/admin/users/10/status")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }
}
