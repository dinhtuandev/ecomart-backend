package com.ecomart.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStoreSettingsRequest {

    @Size(max = 20, message = "Số điện thoại cửa hàng tối đa 20 ký tự")
    private String storePhone;

    @Size(max = 100, message = "Email cửa hàng tối đa 100 ký tự")
    private String storeEmail;

    @Size(max = 255, message = "Địa chỉ cửa hàng tối đa 255 ký tự")
    private String storeAddress;

    private String mapEmbedUrl;
}
