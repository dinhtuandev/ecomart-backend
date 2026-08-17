package com.ecomart.controller;

import com.ecomart.dto.request.AddressRequest;
import com.ecomart.dto.response.AddressResponse;
import com.ecomart.security.JwtAuthenticationFilter;
import com.ecomart.security.UserPrincipal;
import com.ecomart.service.AddressService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AddressController.class)
@AutoConfigureMockMvc(addFilters = false)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AddressService addressService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserPrincipal userPrincipal;
    private UsernamePasswordAuthenticationToken authToken;
    private AddressResponse addressResponse;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.create(10L, "customer@example.com", "pass", "CUSTOMER", true);
        authToken = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        addressResponse = AddressResponse.builder()
                .id(1L)
                .recipientName("Nguyen Van A")
                .recipientPhone("0901234567")
                .addressDetail("123 Street A")
                .ward("Ward 1")
                .district("District 1")
                .province("HCMC")
                .isDefault(true)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/me/addresses trả về danh sách địa chỉ")
    void getAddresses_Success() throws Exception {
        when(addressService.getUserAddresses(10L)).thenReturn(List.of(addressResponse));

        mockMvc.perform(get("/api/v1/me/addresses")
                        .with(authentication(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].recipientName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.data[0].isDefault").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/me/addresses tạo mới địa chỉ trả về HTTP 201")
    void createAddress_Success() throws Exception {
        AddressRequest request = AddressRequest.builder()
                .recipientName("Nguyen Van A")
                .recipientPhone("0901234567")
                .addressDetail("123 Street A")
                .ward("Ward 1")
                .district("District 1")
                .province("HCMC")
                .isDefault(true)
                .build();

        when(addressService.createAddress(eq(10L), any(AddressRequest.class))).thenReturn(addressResponse);

        mockMvc.perform(post("/api/v1/me/addresses")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/me/addresses/{id} trả về HTTP 204 No Content")
    void deleteAddress_Success() throws Exception {
        doNothing().when(addressService).deleteAddress(10L, 1L);

        mockMvc.perform(delete("/api/v1/me/addresses/1")
                        .with(authentication(authToken)))
                .andExpect(status().isNoContent());
    }
}
