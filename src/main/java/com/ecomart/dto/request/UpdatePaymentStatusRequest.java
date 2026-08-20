package com.ecomart.dto.request;

import com.ecomart.entity.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentStatusRequest {

    @NotNull(message = "Trạng thái thanh toán không được để trống")
    private PaymentStatus paymentStatus;
}
