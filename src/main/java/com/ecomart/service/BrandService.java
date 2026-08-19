package com.ecomart.service;

import com.ecomart.dto.request.BrandRequest;
import com.ecomart.dto.response.BrandResponse;

import java.util.List;

public interface BrandService {

    List<BrandResponse> getActiveBrands();

    List<BrandResponse> getAllBrandsForAdmin();

    BrandResponse createBrand(BrandRequest request);

    BrandResponse updateBrand(Long brandId, BrandRequest request);
}
