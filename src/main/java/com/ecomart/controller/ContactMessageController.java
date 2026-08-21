package com.ecomart.controller;

import com.ecomart.dto.request.CreateContactMessageRequest;
import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.ContactMessageResponse;
import com.ecomart.service.ContactMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contact-messages")
@RequiredArgsConstructor
public class ContactMessageController {

    private final ContactMessageService contactMessageService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContactMessageResponse>> createContactMessage(
            @Valid @RequestBody CreateContactMessageRequest request
    ) {
        ContactMessageResponse response = contactMessageService.createMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gửi tin nhắn liên hệ thành công. Chúng tôi sẽ phản hồi sớm nhất có thể.", response));
    }
}
