package com.ecomart.controller;

import com.ecomart.dto.request.UpdateContentPageRequest;
import com.ecomart.dto.response.ContentPageResponse;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.security.UserPrincipal;
import com.ecomart.service.ContentPageService;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminContentPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminContentPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContentPageService contentPageService;

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
    @DisplayName("PATCH /api/v1/admin/pages/{slug} - Admin cập nhật nội dung trang chính sách trả về HTTP 200")
    void updatePage_Returns200() throws Exception {
        UpdateContentPageRequest request = UpdateContentPageRequest.builder()
                .title("Chính sách đổi trả mới")
                .content("Nội dung mới 14 ngày.")
                .build();

        ContentPageResponse response = ContentPageResponse.builder()
                .id(1L)
                .slug(ContentPageService.RETURN_POLICY)
                .title("Chính sách đổi trả mới")
                .content("Nội dung mới 14 ngày.")
                .updatedAt(LocalDateTime.now())
                .build();

        when(contentPageService.updatePage(eq("return-policy"), any(UpdateContentPageRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/pages/return-policy")
                        .with(authentication(adminAuthToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Chính sách đổi trả mới"));
    }
}
