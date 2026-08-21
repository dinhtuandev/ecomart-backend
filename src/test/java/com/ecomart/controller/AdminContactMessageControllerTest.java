package com.ecomart.controller;

import com.ecomart.dto.response.ContactMessageResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.entity.enums.ContactStatus;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.security.UserPrincipal;
import com.ecomart.service.ContactMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminContactMessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminContactMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContactMessageService contactMessageService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserPrincipal adminPrincipal;
    private UsernamePasswordAuthenticationToken adminAuthToken;
    private ContactMessageResponse messageResponse;

    @BeforeEach
    void setUp() {
        adminPrincipal = UserPrincipal.create(99L, "admin@ecomart.com", "pass", "ADMIN", true);
        adminAuthToken = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(adminAuthToken);

        messageResponse = ContactMessageResponse.builder()
                .id(1L)
                .fullName("Nguyen Van A")
                .email("user@example.com")
                .subject("Hỏi về sản phẩm")
                .content("Cho mình hỏi thời gian giao hàng?")
                .status(ContactStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/contact-messages - Admin xem danh sách tin nhắn thành công trả về HTTP 200")
    void getAdminMessages_Returns200() throws Exception {
        PageResponse<ContactMessageResponse> pageResponse = PageResponse.from(List.of(messageResponse), new PageImpl<>(List.of(messageResponse), PageRequest.of(0, 10), 1));

        when(contactMessageService.getAdminMessages(anyInt(), anyInt(), any(), any(), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/contact-messages")
                        .with(authentication(adminAuthToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(1L));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/contact-messages/{id}/resolve - Admin đánh dấu đã xử lý trả về HTTP 200")
    void resolveMessage_Returns200() throws Exception {
        messageResponse.setStatus(ContactStatus.RESOLVED);
        messageResponse.setResolvedAt(LocalDateTime.now());

        when(contactMessageService.resolveMessage(1L)).thenReturn(messageResponse);

        mockMvc.perform(patch("/api/v1/admin/contact-messages/1/resolve")
                        .with(authentication(adminAuthToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }
}
