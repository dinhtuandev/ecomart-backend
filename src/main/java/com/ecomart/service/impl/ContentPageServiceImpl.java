package com.ecomart.service.impl;

import com.ecomart.dto.request.UpdateContentPageRequest;
import com.ecomart.dto.response.ContentPageResponse;
import com.ecomart.entity.ContentPage;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.ContentPageRepository;
import com.ecomart.service.ContentPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentPageServiceImpl implements ContentPageService {

    private final ContentPageRepository contentPageRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ContentPageResponse> getAllPages() {
        return contentPageRepository.findAll().stream()
                .filter(page -> ALLOWED_SLUGS.contains(page.getSlug()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContentPageResponse getPageBySlug(String slug) {
        if (slug == null || !ALLOWED_SLUGS.contains(slug)) {
            throw new ResourceNotFoundException("Trang chính sách không tồn tại với slug: " + slug);
        }

        ContentPage page = contentPageRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Trang chính sách không tồn tại với slug: " + slug));

        return mapToResponse(page);
    }

    @Override
    @Transactional
    public ContentPageResponse updatePage(String slug, UpdateContentPageRequest request) {
        if (slug == null || !ALLOWED_SLUGS.contains(slug)) {
            throw new ResourceNotFoundException("Trang chính sách không tồn tại với slug: " + slug);
        }

        ContentPage page = contentPageRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Trang chính sách không tồn tại với slug: " + slug));

        page.setTitle(request.getTitle().trim());
        page.setContent(request.getContent().trim());
        page.setUpdatedAt(LocalDateTime.now());

        ContentPage updatedPage = contentPageRepository.save(page);
        return mapToResponse(updatedPage);
    }

    private ContentPageResponse mapToResponse(ContentPage page) {
        return ContentPageResponse.builder()
                .id(page.getId())
                .slug(page.getSlug())
                .title(page.getTitle())
                .content(page.getContent())
                .updatedAt(page.getUpdatedAt())
                .build();
    }
}
