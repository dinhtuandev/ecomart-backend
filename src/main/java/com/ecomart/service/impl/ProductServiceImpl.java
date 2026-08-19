package com.ecomart.service.impl;

import com.ecomart.dto.request.ProductImageRequest;
import com.ecomart.dto.request.ProductRequest;
import com.ecomart.dto.response.*;
import com.ecomart.entity.*;
import com.ecomart.exception.BadRequestException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.*;
import com.ecomart.service.ProductService;
import com.ecomart.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final CertificationRepository certificationRepository;
    private final ProductImageRepository productImageRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getPublicProducts(
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
    ) {
        Pageable pageable = createPageable(page, pageSize, sort);
        Specification<Product> spec = ProductSpecification.filterProducts(
                keyword, categoryId, brandId, certificationId, minPrice, maxPrice, minEcoScore, true
        );

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        List<ProductResponse> items = productPage.getContent().stream()
                .map(this::mapToProductResponse)
                .toList();
        return PageResponse.from(items, productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getPublicProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + productId));

        if (!product.isVisible()) {
            throw new ResourceNotFoundException("Sản phẩm không còn hiển thị với ID: " + productId);
        }

        return mapToProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAdminProducts(
            int page,
            int pageSize,
            String keyword,
            Long categoryId,
            Long brandId,
            Boolean isVisible
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Product> spec = ProductSpecification.filterProducts(
                keyword, categoryId, brandId, null, null, null, null, isVisible
        );

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        List<ProductResponse> items = productPage.getContent().stream()
                .map(this::mapToProductResponse)
                .toList();
        return PageResponse.from(items, productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getAdminProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + productId));

        return mapToProductResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        validatePrices(request.getSellingPrice(), request.getOriginalPrice());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BadRequestException("Danh mục không tồn tại với ID: " + request.getCategoryId()));

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new BadRequestException("Thương hiệu không tồn tại với ID: " + request.getBrandId()));

        Set<Certification> certifications = validateAndGetCertifications(request.getCertificationIds());

        Product product = Product.builder()
                .name(request.getName())
                .category(category)
                .brand(brand)
                .sellingPrice(request.getSellingPrice())
                .originalPrice(request.getOriginalPrice())
                .ecoScore(request.getEcoScore())
                .materialInfo(request.getMaterialInfo())
                .description(request.getDescription())
                .isVisible(request.getIsVisible() == null || request.getIsVisible())
                .certifications(certifications)
                .build();

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<ProductImage> images = request.getImages().stream()
                    .map(imgReq -> ProductImage.builder()
                            .product(product)
                            .imageUrl(imgReq.getUrl())
                            .isPrimary(Boolean.TRUE.equals(imgReq.getIsPrimary()))
                            .displayOrder(imgReq.getDisplayOrder() != null ? imgReq.getDisplayOrder() : 0)
                            .build())
                    .toList();
            product.getImages().addAll(images);
        }

        Product savedProduct = productRepository.save(product);

        int initialStock = request.getQuantityInStock() != null ? request.getQuantityInStock() : 0;
        Inventory inventory = Inventory.builder()
                .product(savedProduct)
                .quantity(initialStock)
                .build();
        inventoryRepository.save(inventory);
        savedProduct.setInventory(inventory);

        return mapToProductResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + productId));

        BigDecimal sellingPrice = request.getSellingPrice() != null ? request.getSellingPrice() : product.getSellingPrice();
        BigDecimal originalPrice = request.getOriginalPrice() != null ? request.getOriginalPrice() : product.getOriginalPrice();
        validatePrices(sellingPrice, originalPrice);

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BadRequestException("Danh mục không tồn tại với ID: " + request.getCategoryId()));
            product.setCategory(category);
        }
        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new BadRequestException("Thương hiệu không tồn tại với ID: " + request.getBrandId()));
            product.setBrand(brand);
        }
        if (request.getSellingPrice() != null) {
            product.setSellingPrice(request.getSellingPrice());
        }
        if (request.getOriginalPrice() != null) {
            product.setOriginalPrice(request.getOriginalPrice());
        }
        if (request.getEcoScore() != null) {
            product.setEcoScore(request.getEcoScore());
        }
        if (request.getMaterialInfo() != null) {
            product.setMaterialInfo(request.getMaterialInfo());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getIsVisible() != null) {
            product.setVisible(request.getIsVisible());
        }

        if (request.getCertificationIds() != null) {
            Set<Certification> certifications = validateAndGetCertifications(request.getCertificationIds());
            product.setCertifications(certifications);
        }

        Product updatedProduct = productRepository.save(product);
        return mapToProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProductImages(Long productId, List<ProductImageRequest> imageRequests) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + productId));

        productImageRepository.deleteAllByProductId(productId);
        product.getImages().clear();

        if (imageRequests != null && !imageRequests.isEmpty()) {
            List<ProductImage> newImages = imageRequests.stream()
                    .map(imgReq -> ProductImage.builder()
                            .product(product)
                            .imageUrl(imgReq.getUrl())
                            .isPrimary(Boolean.TRUE.equals(imgReq.getIsPrimary()))
                            .displayOrder(imgReq.getDisplayOrder() != null ? imgReq.getDisplayOrder() : 0)
                            .build())
                    .toList();
            product.getImages().addAll(newImages);
        }

        Product savedProduct = productRepository.save(product);
        return mapToProductResponse(savedProduct);
    }

    private void validatePrices(BigDecimal sellingPrice, BigDecimal originalPrice) {
        if (originalPrice != null && originalPrice.compareTo(sellingPrice) <= 0) {
            throw new BadRequestException("Giá gốc (originalPrice) phải lớn hơn giá bán (sellingPrice)");
        }
    }

    private Set<Certification> validateAndGetCertifications(List<Long> certificationIds) {
        if (certificationIds == null || certificationIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Certification> certs = certificationRepository.findAllById(certificationIds);
        if (certs.size() != certificationIds.size()) {
            throw new BadRequestException("Một hoặc nhiều chứng nhận không tồn tại trong hệ thống");
        }

        for (Certification cert : certs) {
            if (!cert.isActive()) {
                throw new BadRequestException("Chứng nhận '" + cert.getName() + "' (ID: " + cert.getId() + ") hiện không hoạt động");
            }
        }

        return new HashSet<>(certs);
    }

    private Pageable createPageable(int page, int pageSize, String sort) {
        int pageNumber = Math.max(0, page - 1);
        Sort sortObj = Sort.by(Sort.Direction.DESC, "createdAt");

        if ("priceAsc".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.ASC, "sellingPrice");
        } else if ("priceDesc".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.DESC, "sellingPrice");
        }

        return PageRequest.of(pageNumber, pageSize, sortObj);
    }

    private ProductResponse mapToProductResponse(Product product) {
        CategoryResponse categoryResponse = CategoryResponse.builder()
                .id(product.getCategory().getId())
                .name(product.getCategory().getName())
                .description(product.getCategory().getDescription())
                .isActive(product.getCategory().isActive())
                .createdAt(product.getCategory().getCreatedAt())
                .updatedAt(product.getCategory().getUpdatedAt())
                .build();

        BrandResponse brandResponse = BrandResponse.builder()
                .id(product.getBrand().getId())
                .name(product.getBrand().getName())
                .description(product.getBrand().getDescription())
                .isActive(product.getBrand().isActive())
                .createdAt(product.getBrand().getCreatedAt())
                .updatedAt(product.getBrand().getUpdatedAt())
                .build();

        List<CertificationResponse> certResponses = product.getCertifications().stream()
                .map(cert -> CertificationResponse.builder()
                        .id(cert.getId())
                        .name(cert.getName())
                        .description(cert.getDescription())
                        .iconUrl(cert.getIconUrl())
                        .isActive(cert.isActive())
                        .createdAt(cert.getCreatedAt())
                        .updatedAt(cert.getUpdatedAt())
                        .build())
                .toList();

        List<ProductImageResponse> imageResponses = product.getImages().stream()
                .map(img -> ProductImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .isPrimary(img.isPrimary())
                        .displayOrder(img.getDisplayOrder())
                        .createdAt(img.getCreatedAt())
                        .build())
                .toList();

        Integer quantityInStock = product.getInventory() != null ? product.getInventory().getQuantity() : 0;

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .sellingPrice(product.getSellingPrice())
                .originalPrice(product.getOriginalPrice())
                .ecoScore(product.getEcoScore())
                .materialInfo(product.getMaterialInfo())
                .isVisible(product.isVisible())
                .category(categoryResponse)
                .brand(brandResponse)
                .certifications(certResponses)
                .images(imageResponses)
                .quantityInStock(quantityInStock)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
