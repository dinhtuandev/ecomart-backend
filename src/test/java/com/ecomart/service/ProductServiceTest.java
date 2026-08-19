package com.ecomart.service;

import com.ecomart.dto.request.ProductRequest;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.dto.response.ProductResponse;
import com.ecomart.entity.Brand;
import com.ecomart.entity.Category;
import com.ecomart.entity.Inventory;
import com.ecomart.entity.Product;
import com.ecomart.exception.BadRequestException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.*;
import com.ecomart.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private CertificationRepository certificationRepository;
    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category category;
    private Brand brand;
    private Product product;

    @BeforeEach
    void setUp() {
        category = Category.builder().id(1L).name("Gia dụng").build();
        brand = Brand.builder().id(1L).name("EcoLife").build();
        product = Product.builder()
                .id(1L)
                .name("Bình nước inox")
                .category(category)
                .brand(brand)
                .sellingPrice(BigDecimal.valueOf(199000))
                .originalPrice(BigDecimal.valueOf(259000))
                .ecoScore(4)
                .isVisible(true)
                .build();
        product.setInventory(Inventory.builder().id(1L).product(product).quantity(10).build());
    }

    @Test
    @DisplayName("Lấy danh sách sản phẩm public thành công")
    void getPublicProducts_Success() {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        PageResponse<ProductResponse> response = productService.getPublicProducts(
                1, 12, null, null, null, null, null, null, null, "newest"
        );

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getName()).isEqualTo("Bình nước inox");
    }

    @Test
    @DisplayName("Tạo sản phẩm mới thành công và khởi tạo tồn kho")
    void createProduct_Success() {
        ProductRequest request = ProductRequest.builder()
                .name("Bình nước inox")
                .categoryId(1L)
                .brandId(1L)
                .sellingPrice(BigDecimal.valueOf(199000))
                .originalPrice(BigDecimal.valueOf(259000))
                .quantityInStock(10)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(Inventory.builder().quantity(10).build());

        ProductResponse response = productService.createProduct(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Bình nước inox");
        verify(productRepository, times(1)).save(any(Product.class));
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Tạo sản phẩm thất bại khi giá gốc nhỏ hơn hoặc bằng giá bán")
    void createProduct_InvalidPrices_ThrowsBadRequestException() {
        ProductRequest request = ProductRequest.builder()
                .name("Bình nước inox")
                .sellingPrice(BigDecimal.valueOf(200000))
                .originalPrice(BigDecimal.valueOf(150000))
                .build();

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("lớn hơn giá bán");
    }

    @Test
    @DisplayName("Chi tiết sản phẩm public thất bại khi sản phẩm bị ẩn")
    void getPublicProductDetail_HiddenProduct_ThrowsResourceNotFoundException() {
        product.setVisible(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.getPublicProductDetail(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
