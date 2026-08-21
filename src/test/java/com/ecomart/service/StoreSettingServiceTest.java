package com.ecomart.service;

import com.ecomart.dto.request.UpdateStoreSettingsRequest;
import com.ecomart.dto.response.StoreSettingsResponse;
import com.ecomart.entity.StoreSetting;
import com.ecomart.repository.StoreSettingRepository;
import com.ecomart.service.impl.StoreSettingServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreSettingServiceTest {

    @Mock
    private StoreSettingRepository storeSettingRepository;

    @InjectMocks
    private StoreSettingServiceImpl storeSettingService;

    @Test
    @DisplayName("Lấy cấu hình cửa hàng thành công theo whitelist keys")
    void getSettings_Success() {
        StoreSetting phoneSetting = StoreSetting.builder().settingKey(StoreSettingService.KEY_STORE_PHONE).settingValue("0281234567").build();
        StoreSetting emailSetting = StoreSetting.builder().settingKey(StoreSettingService.KEY_STORE_EMAIL).settingValue("contact@ecomart.vn").build();

        when(storeSettingRepository.findAll()).thenReturn(List.of(phoneSetting, emailSetting));

        StoreSettingsResponse response = storeSettingService.getSettings();

        assertThat(response).isNotNull();
        assertThat(response.getStorePhone()).isEqualTo("0281234567");
        assertThat(response.getStoreEmail()).isEqualTo("contact@ecomart.vn");
        assertThat(response.getStoreAddress()).isEmpty();
    }

    @Test
    @DisplayName("Cập nhật cấu hình cửa hàng chỉ upsert các key hợp lệ trong whitelist")
    void updateSettings_Success_OnlyUpsertsWhitelistedKeys() {
        UpdateStoreSettingsRequest request = UpdateStoreSettingsRequest.builder()
                .storePhone("0909999999")
                .storeAddress("456 Eco Street, District 1")
                .build();

        when(storeSettingRepository.findById(StoreSettingService.KEY_STORE_PHONE)).thenReturn(Optional.empty());
        when(storeSettingRepository.findById(StoreSettingService.KEY_STORE_ADDRESS)).thenReturn(Optional.empty());

        StoreSetting phoneSetting = StoreSetting.builder().settingKey(StoreSettingService.KEY_STORE_PHONE).settingValue("0909999999").build();
        StoreSetting addressSetting = StoreSetting.builder().settingKey(StoreSettingService.KEY_STORE_ADDRESS).settingValue("456 Eco Street, District 1").build();
        when(storeSettingRepository.findAll()).thenReturn(List.of(phoneSetting, addressSetting));

        StoreSettingsResponse response = storeSettingService.updateSettings(request);

        assertThat(response).isNotNull();
        assertThat(response.getStorePhone()).isEqualTo("0909999999");
        assertThat(response.getStoreAddress()).isEqualTo("456 Eco Street, District 1");
        verify(storeSettingRepository, times(2)).save(any(StoreSetting.class));
    }
}
