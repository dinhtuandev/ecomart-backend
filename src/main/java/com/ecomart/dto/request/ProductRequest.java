package com.ecomart.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    private String name;

    @NotNull(message = "Danh mục sản phẩm là bắt buộc")
    private Long categoryId;

    @NotNull(message = "Thương hiệu sản phẩm là bắt buộc")
    private Long brandId;

    @NotNull(message = "Giá bán sản phẩm không được để trống")
    @DecimalMin(value = "0.0", message = "Giá bán sản phẩm phải lớn hơn hoặc bằng 0")
    private BigDecimal sellingPrice;

    @DecimalMin(value = "0.0", message = "Giá gốc phải lớn hơn hoặc bằng 0")
    private BigDecimal originalPrice;

    @Min(value = 1, message = "Điểm ecoScore phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm ecoScore phải từ 1 đến 5")
    private Integer ecoScore;

    @Size(max = 255, message = "Thông tin vật liệu không được vượt quá 255 ký tự")
    private String materialInfo;

    private List<Long> certificationIds;

    private String description;

    private Boolean isVisible;

    @Min(value = 0, message = "Số lượng tồn kho phải lớn hơn hoặc bằng 0")
    private Integer quantityInStock;

    private List<ProductImageRequest> images;
}
