package com.ecomart.service;

import com.ecomart.dto.request.UpdateContentPageRequest;
import com.ecomart.dto.response.ContentPageResponse;
import com.ecomart.entity.ContentPage;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.ContentPageRepository;
import com.ecomart.service.impl.ContentPageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentPageServiceTest {

    @Mock
    private ContentPageRepository contentPageRepository;

    @InjectMocks
    private ContentPageServiceImpl contentPageService;

    private ContentPage page;

    @BeforeEach
    void setUp() {
        page = ContentPage.builder()
                .id(1L)
                .slug(ContentPageService.RETURN_POLICY)
                .title("Chính sách đổi trả")
                .content("Nội dung đổi trả 7 ngày.")
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Lấy danh sách tất cả trang chính sách trong whitelist thành công")
    void getAllPages_Success() {
        when(contentPageRepository.findAll()).thenReturn(List.of(page));

        List<ContentPageResponse> response = contentPageService.getAllPages();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getSlug()).isEqualTo(ContentPageService.RETURN_POLICY);
    }

    @Test
    @DisplayName("Lấy chi tiết trang chính sách theo slug hợp lệ thành công")
    void getPageBySlug_Success() {
        when(contentPageRepository.findBySlug(ContentPageService.RETURN_POLICY)).thenReturn(Optional.of(page));

        ContentPageResponse response = contentPageService.getPageBySlug(ContentPageService.RETURN_POLICY);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Chính sách đổi trả");
    }

    @Test
    @DisplayName("Ném 404 khi slug không nằm trong whitelist ALLOWED_SLUGS")
    void getPageBySlug_Throws404_WhenSlugNotInWhitelist() {
        assertThatThrownBy(() -> contentPageService.getPageBySlug("invalid-slug-hack"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Trang chính sách không tồn tại");
    }

    @Test
    @DisplayName("Admin cập nhật nội dung trang chính sách trong whitelist thành công")
    void updatePage_Success() {
        UpdateContentPageRequest request = UpdateContentPageRequest.builder()
                .title("Chính sách đổi trả cập nhật")
                .content("Nội dung mới 14 ngày.")
                .build();

        when(contentPageRepository.findBySlug(ContentPageService.RETURN_POLICY)).thenReturn(Optional.of(page));
        when(contentPageRepository.save(any(ContentPage.class))).thenReturn(page);

        ContentPageResponse response = contentPageService.updatePage(ContentPageService.RETURN_POLICY, request);

        assertThat(response).isNotNull();
        assertThat(page.getTitle()).isEqualTo("Chính sách đổi trả cập nhật");
        assertThat(page.getContent()).isEqualTo("Nội dung mới 14 ngày.");
        verify(contentPageRepository).save(page);
    }

    @Test
    @DisplayName("Ném 404 khi Admin cập nhật trang có slug ngoài whitelist")
    void updatePage_Throws404_WhenSlugNotInWhitelist() {
        UpdateContentPageRequest request = UpdateContentPageRequest.builder().title("Title").content("Content").build();

        assertThatThrownBy(() -> contentPageService.updatePage("unknown-policy", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Trang chính sách không tồn tại");
    }
}
