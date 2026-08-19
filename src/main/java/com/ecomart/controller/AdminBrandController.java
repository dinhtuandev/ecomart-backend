package com.ecomart.controller;

import com.ecomart.dto.request.BrandRequest;
import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.BrandResponse;
import com.ecomart.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/brands")
@RequiredArgsConstructor
public class AdminBrandController {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAllBrandsForAdmin() {
        List<BrandResponse> response = brandService.getAllBrandsForAdmin();
        return ResponseEntity.ok(ApiResponse.success("Lấy tất cả thương hiệu thành công.", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(@Valid @RequestBody BrandRequest request) {
        BrandResponse response = brandService.createBrand(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo thương hiệu mới thành công.", response));
    }

    @PatchMapping("/{brandId}")
    public ResponseEntity<ApiResponse<BrandResponse>> updateBrand(@PathVariable Long brandId,
                                                                   @RequestBody BrandRequest request) {
        BrandResponse response = brandService.updateBrand(brandId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thương hiệu thành công.", response));
    }
}
