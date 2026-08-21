package com.ecomart.controller;

import com.ecomart.dto.request.CreateContactMessageRequest;
import com.ecomart.dto.response.ContactMessageResponse;
import com.ecomart.entity.enums.ContactStatus;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.JwtTokenProvider;
import com.ecomart.service.ContactMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactMessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContactMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContactMessageService contactMessageService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST /api/v1/contact-messages - Public không cần JWT gửi tin nhắn liên hệ thành công trả về HTTP 201")
    void createContactMessage_PublicWithoutJwt_Returns201() throws Exception {
        CreateContactMessageRequest request = CreateContactMessageRequest.builder()
                .fullName("Nguyen Van A")
                .email("user@example.com")
                .subject("Hỏi về sản phẩm")
                .content("Cho mình hỏi thời gian giao hàng dự kiến?")
                .build();

        ContactMessageResponse response = ContactMessageResponse.builder()
                .id(1L)
                .fullName("Nguyen Van A")
                .email("user@example.com")
                .subject("Hỏi về sản phẩm")
                .content("Cho mình hỏi thời gian giao hàng dự kiến?")
                .status(ContactStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();

        when(contactMessageService.createMessage(any(CreateContactMessageRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/contact-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.status").value("NEW"));
    }
}
