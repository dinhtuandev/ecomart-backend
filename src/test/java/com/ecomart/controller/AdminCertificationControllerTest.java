package com.ecomart.controller;

import com.ecomart.dto.request.CertificationRequest;
import com.ecomart.dto.response.CertificationResponse;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.service.CertificationService;
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

@WebMvcTest(AdminCertificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCertificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CertificationService certificationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /api/v1/admin/certifications trả về HTTP 200")
    void getAllCertifications_Returns200() throws Exception {
        CertificationResponse response = CertificationResponse.builder().id(1L).name("USDA Organic").build();

        when(certificationService.getAllCertificationsForAdmin()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/admin/certifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/admin/certifications trả về HTTP 201 khi dữ liệu hợp lệ")
    void createCertification_Success_Returns201() throws Exception {
        CertificationRequest request = CertificationRequest.builder().name("USDA Organic").build();
        CertificationResponse response = CertificationResponse.builder().id(1L).name("USDA Organic").build();

        when(certificationService.createCertification(any(CertificationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/certifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/certifications/{id} trả về HTTP 200")
    void updateCertification_Success_Returns200() throws Exception {
        CertificationRequest request = CertificationRequest.builder().name("USDA Organic Updated").build();
        CertificationResponse response = CertificationResponse.builder().id(1L).name("USDA Organic Updated").build();

        when(certificationService.updateCertification(eq(1L), any(CertificationRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/certifications/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
