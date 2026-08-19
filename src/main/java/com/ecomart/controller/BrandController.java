package com.ecomart.controller;

import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.BrandResponse;
import com.ecomart.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getActiveBrands() {
        List<BrandResponse> response = brandService.getActiveBrands();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thương hiệu thành công.", response));
    }
}
