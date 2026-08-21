package com.ecomart.repository;

import com.ecomart.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT oi.product.id, oi.productName, SUM(oi.quantity), SUM(oi.lineTotal) " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE o.status = com.ecomart.entity.enums.OrderStatus.COMPLETED " +
            "AND o.completedAt >= :start AND o.completedAt <= :end " +
            "GROUP BY oi.product.id, oi.productName " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findTopSellingProductsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    @Query("SELECT c.id, c.name, SUM(oi.quantity), SUM(oi.lineTotal) " +
            "FROM OrderItem oi JOIN oi.order o JOIN oi.product p JOIN p.category c " +
            "WHERE o.status = com.ecomart.entity.enums.OrderStatus.COMPLETED " +
            "AND o.completedAt >= :start AND o.completedAt <= :end " +
            "GROUP BY c.id, c.name ORDER BY SUM(oi.lineTotal) DESC")
    List<Object[]> getSalesByCategoryBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT b.id, b.name, SUM(oi.quantity), SUM(oi.lineTotal) " +
            "FROM OrderItem oi JOIN oi.order o JOIN oi.product p JOIN p.brand b " +
            "WHERE o.status = com.ecomart.entity.enums.OrderStatus.COMPLETED " +
            "AND o.completedAt >= :start AND o.completedAt <= :end " +
            "GROUP BY b.id, b.name ORDER BY SUM(oi.lineTotal) DESC")
    List<Object[]> getSalesByBrandBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT AVG(p.ecoScore), " +
            "SUM(CASE WHEN p.ecoScore >= 4 THEN oi.quantity ELSE 0 END) " +
            "FROM OrderItem oi JOIN oi.order o JOIN oi.product p " +
            "WHERE o.status = com.ecomart.entity.enums.OrderStatus.COMPLETED")
    List<Object[]> getEcoImpactSummary();
}
