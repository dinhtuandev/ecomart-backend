package com.ecomart.controller;

import com.ecomart.dto.request.UpdateStoreSettingsRequest;
import com.ecomart.dto.response.StoreSettingsResponse;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.security.UserPrincipal;
import com.ecomart.service.StoreSettingService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminStoreSettingController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminStoreSettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StoreSettingService storeSettingService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserPrincipal adminPrincipal;
    private UsernamePasswordAuthenticationToken adminAuthToken;

    @BeforeEach
    void setUp() {
        adminPrincipal = UserPrincipal.create(99L, "admin@ecomart.com", "pass", "ADMIN", true);
        adminAuthToken = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(adminAuthToken);
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/settings - Admin cập nhật cấu hình cửa hàng trả về HTTP 200")
    void updateStoreSettings_Returns200() throws Exception {
        UpdateStoreSettingsRequest request = UpdateStoreSettingsRequest.builder()
                .storePhone("0909123456")
                .storeEmail("admin@ecomart.vn")
                .build();

        StoreSettingsResponse response = StoreSettingsResponse.builder()
                .storePhone("0909123456")
                .storeEmail("admin@ecomart.vn")
                .storeAddress("12 Đường A, Phường B, Quận C, TP. HCM")
                .mapEmbedUrl("https://maps.google.com/embed")
                .build();

        when(storeSettingService.updateSettings(any(UpdateStoreSettingsRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/settings")
                        .with(authentication(adminAuthToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.storePhone").value("0909123456"))
                .andExpect(jsonPath("$.data.storeEmail").value("admin@ecomart.vn"));
    }
}
