package com.ecomart.specification;

import com.ecomart.entity.Review;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ReviewSpecification {

    public static Specification<Review> filterAdminReviews(
            Long productId,
            Integer rating,
            Boolean isVisible,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (productId != null) {
                predicates.add(criteriaBuilder.equal(root.get("product").get("id"), productId));
            }

            if (rating != null) {
                predicates.add(criteriaBuilder.equal(root.get("rating"), rating));
            }

            if (isVisible != null) {
                predicates.add(criteriaBuilder.equal(root.get("isVisible"), isVisible));
            }

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate userPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("user").get("fullName")), pattern);
                Predicate productPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("product").get("name")), pattern);
                Predicate commentPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("comment")), pattern);
                predicates.add(criteriaBuilder.or(userPredicate, productPredicate, commentPredicate));
            }

            if (fromDate != null) {
                LocalDateTime startOfDay = fromDate.atStartOfDay();
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startOfDay));
            }

            if (toDate != null) {
                LocalDateTime endOfDay = toDate.atTime(LocalTime.MAX);
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endOfDay));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
