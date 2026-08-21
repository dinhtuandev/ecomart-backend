package com.ecomart.controller;

import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.ContactMessageResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.entity.enums.ContactStatus;
import com.ecomart.service.ContactMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/contact-messages")
@RequiredArgsConstructor
public class AdminContactMessageController {

    private final ContactMessageService contactMessageService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ContactMessageResponse>>> getAdminMessages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) ContactStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        PageResponse<ContactMessageResponse> response = contactMessageService.getAdminMessages(
                page, pageSize, status, keyword, fromDate, toDate
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tin nhắn liên hệ thành công.", response));
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> getAdminMessageDetail(@PathVariable Long messageId) {
        ContactMessageResponse response = contactMessageService.getAdminMessageDetail(messageId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết tin nhắn liên hệ thành công.", response));
    }

    @PatchMapping("/{messageId}/resolve")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> resolveMessage(@PathVariable Long messageId) {
        ContactMessageResponse response = contactMessageService.resolveMessage(messageId);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tin nhắn liên hệ là đã xử lý.", response));
    }
}
