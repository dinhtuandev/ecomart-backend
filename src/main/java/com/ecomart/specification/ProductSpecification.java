package com.ecomart.specification;

import com.ecomart.entity.Certification;
import com.ecomart.entity.Product;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> filterProducts(
            String keyword,
            Long categoryId,
            Long brandId,
            Long certificationId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minEcoScore,
            Boolean isVisible
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (Boolean.TRUE.equals(isVisible)) {
                predicates.add(criteriaBuilder.equal(root.get("isVisible"), true));
            } else if (Boolean.FALSE.equals(isVisible)) {
                predicates.add(criteriaBuilder.equal(root.get("isVisible"), false));
            }

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern));
            }

            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            if (brandId != null) {
                predicates.add(criteriaBuilder.equal(root.get("brand").get("id"), brandId));
            }

            if (certificationId != null) {
                Join<Product, Certification> certificationJoin = root.join("certifications");
                predicates.add(criteriaBuilder.equal(certificationJoin.get("id"), certificationId));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("sellingPrice"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("sellingPrice"), maxPrice));
            }

            if (minEcoScore != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("ecoScore"), minEcoScore));
            }

            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
