package com.ecomart.controller;

import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.StoreSettingsResponse;
import com.ecomart.service.StoreSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class StoreSettingController {

    private final StoreSettingService storeSettingService;

    @GetMapping
    public ResponseEntity<ApiResponse<StoreSettingsResponse>> getStoreSettings() {
        StoreSettingsResponse response = storeSettingService.getSettings();
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cấu hình cửa hàng thành công.", response));
    }
}
