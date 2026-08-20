package com.ecomart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCancelOrderRequest {

    @NotBlank(message = "Lý do hủy đơn hàng không được để trống")
    private String cancellationReason;
}
