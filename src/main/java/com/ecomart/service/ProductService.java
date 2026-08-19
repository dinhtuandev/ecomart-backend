package com.ecomart.service;

import com.ecomart.dto.request.ProductImageRequest;
import com.ecomart.dto.request.ProductRequest;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.dto.response.ProductResponse;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    PageResponse<ProductResponse> getPublicProducts(
            int page,
            int pageSize,
            String keyword,
            Long categoryId,
            Long brandId,
            Long certificationId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minEcoScore,
            String sort
    );

    ProductResponse getPublicProductDetail(Long productId);

    PageResponse<ProductResponse> getAdminProducts(
            int page,
            int pageSize,
            String keyword,
            Long categoryId,
            Long brandId,
            Boolean isVisible
    );

    ProductResponse getAdminProductDetail(Long productId);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long productId, ProductRequest request);

    ProductResponse updateProductImages(Long productId, List<ProductImageRequest> imageRequests);
}
