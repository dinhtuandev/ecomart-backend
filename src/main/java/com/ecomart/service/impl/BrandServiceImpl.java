package com.ecomart.service.impl;

import com.ecomart.dto.request.BrandRequest;
import com.ecomart.dto.response.BrandResponse;
import com.ecomart.entity.Brand;
import com.ecomart.exception.BadRequestException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.BrandRepository;
import com.ecomart.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> getActiveBrands() {
        return brandRepository.findAllByIsActiveTrueOrderByNameAsc().stream()
                .map(this::mapToBrandResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrandsForAdmin() {
        return brandRepository.findAllByOrderByNameAsc().stream()
                .map(this::mapToBrandResponse)
                .toList();
    }

    @Override
    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        if (brandRepository.existsByName(request.getName())) {
            throw new BadRequestException("Tên thương hiệu '" + request.getName() + "' đã tồn tại");
        }

        Brand brand = Brand.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();

        Brand savedBrand = brandRepository.save(brand);
        return mapToBrandResponse(savedBrand);
    }

    @Override
    @Transactional
    public BrandResponse updateBrand(Long brandId, BrandRequest request) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu không tồn tại với ID: " + brandId));

        if (request.getName() != null && brandRepository.existsByNameAndIdNot(request.getName(), brandId)) {
            throw new BadRequestException("Tên thương hiệu '" + request.getName() + "' đã được sử dụng bởi thương hiệu khác");
        }

        if (request.getName() != null) {
            brand.setName(request.getName());
        }
        if (request.getDescription() != null) {
            brand.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            brand.setActive(request.getIsActive());
        }

        Brand updatedBrand = brandRepository.save(brand);
        return mapToBrandResponse(updatedBrand);
    }

    private BrandResponse mapToBrandResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .description(brand.getDescription())
                .isActive(brand.isActive())
                .createdAt(brand.getCreatedAt())
                .updatedAt(brand.getUpdatedAt())
                .build();
    }
}
