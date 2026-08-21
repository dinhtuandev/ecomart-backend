package com.ecomart.service;

import com.ecomart.dto.request.UpdateContentPageRequest;
import com.ecomart.dto.response.ContentPageResponse;

import java.util.List;
import java.util.Set;

public interface ContentPageService {

    String RETURN_POLICY = "return-policy";
    String WARRANTY_POLICY = "warranty-policy";
    String SHIPPING_POLICY = "shipping-policy";
    Set<String> ALLOWED_SLUGS = Set.of(RETURN_POLICY, WARRANTY_POLICY, SHIPPING_POLICY);

    List<ContentPageResponse> getAllPages();

    ContentPageResponse getPageBySlug(String slug);

    ContentPageResponse updatePage(String slug, UpdateContentPageRequest request);
}
