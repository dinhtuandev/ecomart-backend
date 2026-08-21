package com.ecomart.controller;

import com.ecomart.dto.response.ContentPageResponse;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.service.ContentPageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContentPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContentPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContentPageService contentPageService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /api/v1/pages - Public không cần JWT xem danh sách các trang chính sách trả về HTTP 200")
    void getAllPages_Returns200() throws Exception {
        ContentPageResponse page = ContentPageResponse.builder()
                .id(1L)
                .slug(ContentPageService.RETURN_POLICY)
                .title("Chính sách đổi trả")
                .content("Nội dung 7 ngày.")
                .updatedAt(LocalDateTime.now())
                .build();

        when(contentPageService.getAllPages()).thenReturn(List.of(page));

        mockMvc.perform(get("/api/v1/pages")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].slug").value("return-policy"));
    }

    @Test
    @DisplayName("GET /api/v1/pages/{slug} - Public xem chi tiết trang theo slug trả về HTTP 200")
    void getPageBySlug_Returns200() throws Exception {
        ContentPageResponse page = ContentPageResponse.builder()
                .id(1L)
                .slug(ContentPageService.RETURN_POLICY)
                .title("Chính sách đổi trả")
                .content("Nội dung 7 ngày.")
                .updatedAt(LocalDateTime.now())
                .build();

        when(contentPageService.getPageBySlug("return-policy")).thenReturn(page);

        mockMvc.perform(get("/api/v1/pages/return-policy")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("return-policy"));
    }

    @Test
    @DisplayName("GET /api/v1/pages/{slug} - Trả về 404 khi slug không tồn tại hoặc không trong whitelist")
    void getPageBySlug_Returns404_WhenNotFound() throws Exception {
        when(contentPageService.getPageBySlug("invalid-slug"))
                .thenThrow(new ResourceNotFoundException("Trang chính sách không tồn tại"));

        mockMvc.perform(get("/api/v1/pages/invalid-slug")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
