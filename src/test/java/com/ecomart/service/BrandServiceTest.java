package com.ecomart.service;

import com.ecomart.dto.request.BrandRequest;
import com.ecomart.dto.response.BrandResponse;
import com.ecomart.entity.Brand;
import com.ecomart.exception.BadRequestException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.BrandRepository;
import com.ecomart.service.impl.BrandServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandServiceImpl brandService;

    private Brand activeBrand;

    @BeforeEach
    void setUp() {
        activeBrand = Brand.builder()
                .id(1L)
                .name("EcoLife")
                .description("Thương hiệu xanh")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Lấy danh sách thương hiệu active thành công")
    void getActiveBrands_Success() {
        when(brandRepository.findAllByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(activeBrand));

        List<BrandResponse> responses = brandService.getActiveBrands();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("EcoLife");
    }

    @Test
    @DisplayName("Tạo thương hiệu mới thành công khi tên chưa tồn tại")
    void createBrand_Success() {
        BrandRequest request = BrandRequest.builder()
                .name("GreenTech")
                .description("Công nghệ xanh")
                .isActive(true)
                .build();

        when(brandRepository.existsByName("GreenTech")).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenReturn(Brand.builder()
                .id(2L)
                .name("GreenTech")
                .description("Công nghệ xanh")
                .isActive(true)
                .build());

        BrandResponse response = brandService.createBrand(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("GreenTech");
        verify(brandRepository, times(1)).save(any(Brand.class));
    }

    @Test
    @DisplayName("Tạo thương hiệu thất bại khi trùng tên")
    void createBrand_DuplicateName_ThrowsBadRequestException() {
        BrandRequest request = BrandRequest.builder()
                .name("EcoLife")
                .build();

        when(brandRepository.existsByName("EcoLife")).thenReturn(true);

        assertThatThrownBy(() -> brandService.createBrand(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    @DisplayName("Cập nhật thương hiệu thành công")
    void updateBrand_Success() {
        BrandRequest request = BrandRequest.builder()
                .name("EcoLife Vietnam")
                .isActive(true)
                .build();

        when(brandRepository.findById(1L)).thenReturn(Optional.of(activeBrand));
        when(brandRepository.existsByNameAndIdNot("EcoLife Vietnam", 1L)).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenReturn(activeBrand);

        BrandResponse response = brandService.updateBrand(1L, request);

        assertThat(response).isNotNull();
        verify(brandRepository, times(1)).save(activeBrand);
    }

    @Test
    @DisplayName("Cập nhật thương hiệu thất bại khi không tìm thấy ID")
    void updateBrand_NotFound_ThrowsException() {
        BrandRequest request = BrandRequest.builder().name("Test").build();

        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.updateBrand(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
