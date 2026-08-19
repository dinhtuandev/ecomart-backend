package com.ecomart.service;

import com.ecomart.dto.request.CategoryRequest;
import com.ecomart.dto.response.CategoryResponse;
import com.ecomart.entity.Category;
import com.ecomart.exception.BadRequestException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.CategoryRepository;
import com.ecomart.service.impl.CategoryServiceImpl;
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
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category activeCategory;

    @BeforeEach
    void setUp() {
        activeCategory = Category.builder()
                .id(1L)
                .name("Thực phẩm hữu cơ")
                .description("Đồ ăn hữu cơ")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Lấy danh sách danh mục active thành công")
    void getActiveCategories_Success() {
        when(categoryRepository.findAllByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(activeCategory));

        List<CategoryResponse> responses = categoryService.getActiveCategories();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Thực phẩm hữu cơ");
    }

    @Test
    @DisplayName("Tạo danh mục mới thành công khi tên chưa tồn tại")
    void createCategory_Success() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Đồ gia dụng xanh")
                .description("Đồ thân thiện môi trường")
                .isActive(true)
                .build();

        when(categoryRepository.existsByName("Đồ gia dụng xanh")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(Category.builder()
                .id(2L)
                .name("Đồ gia dụng xanh")
                .description("Đồ thân thiện môi trường")
                .isActive(true)
                .build());

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Đồ gia dụng xanh");
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Tạo danh mục thất bại khi trùng tên")
    void createCategory_DuplicateName_ThrowsBadRequestException() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Thực phẩm hữu cơ")
                .build();

        when(categoryRepository.existsByName("Thực phẩm hữu cơ")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    @DisplayName("Cập nhật danh mục thành công")
    void updateCategory_Success() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Thực phẩm hữu cơ mới")
                .isActive(false)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));
        when(categoryRepository.existsByNameAndIdNot("Thực phẩm hữu cơ mới", 1L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(activeCategory);

        CategoryResponse response = categoryService.updateCategory(1L, request);

        assertThat(response).isNotNull();
        verify(categoryRepository, times(1)).save(activeCategory);
    }

    @Test
    @DisplayName("Cập nhật danh mục thất bại khi không tìm thấy ID")
    void updateCategory_NotFound_ThrowsException() {
        CategoryRequest request = CategoryRequest.builder().name("Test").build();

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
