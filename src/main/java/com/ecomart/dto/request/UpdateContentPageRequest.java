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
public class UpdateContentPageRequest {

    @NotBlank(message = "Tiêu đề trang không được để trống")
    @Size(max = 255, message = "Tiêu đề trang tối đa 255 ký tự")
    private String title;

    @NotBlank(message = "Nội dung trang không được để trống")
    private String content;
}
