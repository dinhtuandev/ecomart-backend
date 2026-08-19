package com.ecomart.controller;

import com.ecomart.dto.request.CertificationRequest;
import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.CertificationResponse;
import com.ecomart.service.CertificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/certifications")
@RequiredArgsConstructor
public class AdminCertificationController {

    private final CertificationService certificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CertificationResponse>>> getAllCertificationsForAdmin() {
        List<CertificationResponse> response = certificationService.getAllCertificationsForAdmin();
        return ResponseEntity.ok(ApiResponse.success("Lấy tất cả chứng nhận sinh thái thành công.", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CertificationResponse>> createCertification(@Valid @RequestBody CertificationRequest request) {
        CertificationResponse response = certificationService.createCertification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo chứng nhận sinh thái mới thành công.", response));
    }

    @PatchMapping("/{certificationId}")
    public ResponseEntity<ApiResponse<CertificationResponse>> updateCertification(@PathVariable Long certificationId,
                                                                                   @RequestBody CertificationRequest request) {
        CertificationResponse response = certificationService.updateCertification(certificationId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật chứng nhận sinh thái thành công.", response));
    }
}
