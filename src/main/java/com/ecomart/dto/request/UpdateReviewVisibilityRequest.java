package com.ecomart.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewVisibilityRequest {

    @NotNull(message = "Trạng thái hiển thị không được để trống")
    private Boolean isVisible;
}
