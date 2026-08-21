package com.ecomart.service;

import com.ecomart.dto.request.UpdateStoreSettingsRequest;
import com.ecomart.dto.response.StoreSettingsResponse;

import java.util.Set;

public interface StoreSettingService {

    String KEY_STORE_PHONE = "storePhone";
    String KEY_STORE_EMAIL = "storeEmail";
    String KEY_STORE_ADDRESS = "storeAddress";
    String KEY_MAP_EMBED_URL = "mapEmbedUrl";
    Set<String> ALLOWED_SETTING_KEYS = Set.of(KEY_STORE_PHONE, KEY_STORE_EMAIL, KEY_STORE_ADDRESS, KEY_MAP_EMBED_URL);

    StoreSettingsResponse getSettings();

    StoreSettingsResponse updateSettings(UpdateStoreSettingsRequest request);
}
