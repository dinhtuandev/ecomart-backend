package com.ecomart.service.impl;

import com.ecomart.dto.request.UpdateInventoryRequest;
import com.ecomart.dto.response.InventoryResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.entity.Inventory;
import com.ecomart.entity.Product;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.InventoryRepository;
import com.ecomart.repository.ProductRepository;
import com.ecomart.service.InventoryService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getAdminInventory(int page, int pageSize, String keyword, Boolean lowStockOnly) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Specification<Inventory> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Inventory, Product> productJoin = root.join("product");

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(productJoin.get("name")), pattern));
            }

            if (Boolean.TRUE.equals(lowStockOnly)) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("quantity"), 5));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Inventory> inventoryPage = inventoryRepository.findAll(spec, pageable);
        List<InventoryResponse> items = inventoryPage.getContent().stream()
                .map(this::mapToInventoryResponse)
                .toList();
        return PageResponse.from(items, inventoryPage);
    }

    @Override
    @Transactional
    public InventoryResponse updateStock(Long productId, UpdateInventoryRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> {
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + productId));
                    return Inventory.builder().product(product).quantity(0).build();
                });

        inventory.setQuantity(request.getQuantityInStock());
        Inventory savedInventory = inventoryRepository.save(inventory);

        return mapToInventoryResponse(savedInventory);
    }

    private InventoryResponse mapToInventoryResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct().getId())
                .productName(inventory.getProduct().getName())
                .quantityInStock(inventory.getQuantity())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
