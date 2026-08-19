package com.ecomart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificationRequest {

    @NotBlank(message = "Tên chứng nhận không được để trống")
    @Size(max = 100, message = "Tên chứng nhận không được vượt quá 100 ký tự")
    private String name;

    @Size(max = 255, message = "Mô tả chứng nhận không được vượt quá 255 ký tự")
    private String description;

    @Size(max = 255, message = "Đường dẫn biểu tượng không được vượt quá 255 ký tự")
    private String iconUrl;

    private Boolean isActive;
}
