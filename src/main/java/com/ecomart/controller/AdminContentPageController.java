package com.ecomart.controller;

import com.ecomart.dto.request.UpdateContentPageRequest;
import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.ContentPageResponse;
import com.ecomart.service.ContentPageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/pages")
@RequiredArgsConstructor
public class AdminContentPageController {

    private final ContentPageService contentPageService;

    @PatchMapping("/{slug}")
    public ResponseEntity<ApiResponse<ContentPageResponse>> updatePage(
            @PathVariable String slug,
            @Valid @RequestBody UpdateContentPageRequest request
    ) {
        ContentPageResponse response = contentPageService.updatePage(slug, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật nội dung trang chính sách thành công.", response));
    }
}
