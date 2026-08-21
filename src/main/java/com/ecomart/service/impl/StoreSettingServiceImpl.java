package com.ecomart.service.impl;

import com.ecomart.dto.request.UpdateStoreSettingsRequest;
import com.ecomart.dto.response.StoreSettingsResponse;
import com.ecomart.entity.StoreSetting;
import com.ecomart.repository.StoreSettingRepository;
import com.ecomart.service.StoreSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreSettingServiceImpl implements StoreSettingService {

    private final StoreSettingRepository storeSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public StoreSettingsResponse getSettings() {
        Map<String, String> settingsMap = storeSettingRepository.findAll().stream()
                .filter(s -> ALLOWED_SETTING_KEYS.contains(s.getSettingKey()))
                .collect(Collectors.toMap(StoreSetting::getSettingKey, s -> s.getSettingValue() != null ? s.getSettingValue() : ""));

        return StoreSettingsResponse.builder()
                .storePhone(settingsMap.getOrDefault(KEY_STORE_PHONE, ""))
                .storeEmail(settingsMap.getOrDefault(KEY_STORE_EMAIL, ""))
                .storeAddress(settingsMap.getOrDefault(KEY_STORE_ADDRESS, ""))
                .mapEmbedUrl(settingsMap.getOrDefault(KEY_MAP_EMBED_URL, ""))
                .build();
    }

    @Override
    @Transactional
    public StoreSettingsResponse updateSettings(UpdateStoreSettingsRequest request) {
        if (request.getStorePhone() != null) {
            upsertSetting(KEY_STORE_PHONE, request.getStorePhone().trim());
        }
        if (request.getStoreEmail() != null) {
            upsertSetting(KEY_STORE_EMAIL, request.getStoreEmail().trim());
        }
        if (request.getStoreAddress() != null) {
            upsertSetting(KEY_STORE_ADDRESS, request.getStoreAddress().trim());
        }
        if (request.getMapEmbedUrl() != null) {
            upsertSetting(KEY_MAP_EMBED_URL, request.getMapEmbedUrl().trim());
        }

        return getSettings();
    }

    private void upsertSetting(String key, String value) {
        StoreSetting setting = storeSettingRepository.findById(key)
                .orElseGet(() -> StoreSetting.builder().settingKey(key).build());

        setting.setSettingValue(value);
        setting.setUpdatedAt(LocalDateTime.now());
        storeSettingRepository.save(setting);
    }
}
