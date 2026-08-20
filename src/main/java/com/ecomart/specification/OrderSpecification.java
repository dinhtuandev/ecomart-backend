package com.ecomart.specification;

import com.ecomart.entity.Order;
import com.ecomart.entity.enums.OrderStatus;
import com.ecomart.entity.enums.PaymentMethod;
import com.ecomart.entity.enums.PaymentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {

    public static Specification<Order> filterAdminOrders(
            String keyword,
            OrderStatus status,
            PaymentStatus paymentStatus,
            PaymentMethod paymentMethod,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate codePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("orderCode")), pattern);
                Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("recipientName")), pattern);
                Predicate phonePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("recipientPhone")), pattern);
                predicates.add(criteriaBuilder.or(codePredicate, namePredicate, phonePredicate));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (paymentStatus != null) {
                predicates.add(criteriaBuilder.equal(root.get("paymentStatus"), paymentStatus));
            }

            if (paymentMethod != null) {
                predicates.add(criteriaBuilder.equal(root.get("paymentMethod"), paymentMethod));
            }

            if (fromDate != null) {
                LocalDateTime startOfDay = fromDate.atStartOfDay();
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("orderedAt"), startOfDay));
            }

            if (toDate != null) {
                LocalDateTime endOfDay = toDate.atTime(LocalTime.MAX);
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("orderedAt"), endOfDay));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
