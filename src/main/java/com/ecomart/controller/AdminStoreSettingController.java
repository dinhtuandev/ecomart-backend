package com.ecomart.controller;

import com.ecomart.dto.request.UpdateStoreSettingsRequest;
import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.StoreSettingsResponse;
import com.ecomart.service.StoreSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
public class AdminStoreSettingController {

    private final StoreSettingService storeSettingService;

    @PatchMapping
    public ResponseEntity<ApiResponse<StoreSettingsResponse>> updateStoreSettings(
            @Valid @RequestBody UpdateStoreSettingsRequest request
    ) {
        StoreSettingsResponse response = storeSettingService.updateSettings(request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cấu hình cửa hàng thành công.", response));
    }
}
