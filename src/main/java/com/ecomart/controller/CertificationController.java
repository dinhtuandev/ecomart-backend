package com.ecomart.controller;

import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.CertificationResponse;
import com.ecomart.service.CertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/certifications")
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService certificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CertificationResponse>>> getActiveCertifications() {
        List<CertificationResponse> response = certificationService.getActiveCertifications();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chứng nhận sinh thái thành công.", response));
    }
}
