package com.ecomart.controller;

import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.ContentPageResponse;
import com.ecomart.service.ContentPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pages")
@RequiredArgsConstructor
public class ContentPageController {

    private final ContentPageService contentPageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ContentPageResponse>>> getAllPages() {
        List<ContentPageResponse> response = contentPageService.getAllPages();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách các trang chính sách thành công.", response));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ContentPageResponse>> getPageBySlug(@PathVariable String slug) {
        ContentPageResponse response = contentPageService.getPageBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết trang chính sách thành công.", response));
    }
}
