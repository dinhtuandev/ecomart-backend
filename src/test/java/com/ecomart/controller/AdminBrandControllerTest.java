package com.ecomart.controller;

import com.ecomart.dto.request.BrandRequest;
import com.ecomart.dto.response.BrandResponse;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.service.BrandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminBrandController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminBrandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BrandService brandService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /api/v1/admin/brands trả về HTTP 200")
    void getAllBrands_Returns200() throws Exception {
        BrandResponse response = BrandResponse.builder().id(1L).name("Test Brand").build();

        when(brandService.getAllBrandsForAdmin()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/admin/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/admin/brands trả về HTTP 201 khi dữ liệu hợp lệ")
    void createBrand_Success_Returns201() throws Exception {
        BrandRequest request = BrandRequest.builder().name("Thương Hiệu Mới").build();
        BrandResponse response = BrandResponse.builder().id(1L).name("Thương Hiệu Mới").build();

        when(brandService.createBrand(any(BrandRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/brands/{id} trả về HTTP 200")
    void updateBrand_Success_Returns200() throws Exception {
        BrandRequest request = BrandRequest.builder().name("Cập nhật Thương Hiệu").build();
        BrandResponse response = BrandResponse.builder().id(1L).name("Cập nhật Thương Hiệu").build();

        when(brandService.updateBrand(eq(1L), any(BrandRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/brands/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
