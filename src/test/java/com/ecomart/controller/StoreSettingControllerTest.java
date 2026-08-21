package com.ecomart.controller;

import com.ecomart.dto.response.StoreSettingsResponse;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.service.StoreSettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StoreSettingController.class)
@AutoConfigureMockMvc(addFilters = false)
class StoreSettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StoreSettingService storeSettingService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /api/v1/settings - Public không cần JWT xem thông tin cấu hình cửa hàng trả về HTTP 200")
    void getStoreSettings_Returns200() throws Exception {
        StoreSettingsResponse response = StoreSettingsResponse.builder()
                .storePhone("0281234567")
                .storeEmail("contact@ecomart.vn")
                .storeAddress("12 Đường A, Phường B, Quận C, TP. HCM")
                .mapEmbedUrl("https://maps.google.com/embed")
                .build();

        when(storeSettingService.getSettings()).thenReturn(response);

        mockMvc.perform(get("/api/v1/settings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.storePhone").value("0281234567"))
                .andExpect(jsonPath("$.data.storeEmail").value("contact@ecomart.vn"));
    }
}
