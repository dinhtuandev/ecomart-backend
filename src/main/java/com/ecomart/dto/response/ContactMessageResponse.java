package com.ecomart.dto.response;

import com.ecomart.entity.enums.ContactStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String subject;
    private String content;
    private ContactStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
