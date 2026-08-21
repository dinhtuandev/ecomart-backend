package com.ecomart.repository;

import com.ecomart.entity.Order;
import com.ecomart.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Optional<Order> findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

    long countByStatus(OrderStatus status);

    List<Order> findTop5ByOrderByOrderedAtDesc();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = com.ecomart.entity.enums.OrderStatus.COMPLETED")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT YEAR(o.completedAt), MONTH(o.completedAt), DAY(o.completedAt), COALESCE(SUM(o.totalAmount), 0), COUNT(o.id) " +
            "FROM Order o " +
            "WHERE o.status = com.ecomart.entity.enums.OrderStatus.COMPLETED " +
            "AND o.completedAt >= :start AND o.completedAt <= :end " +
            "GROUP BY YEAR(o.completedAt), MONTH(o.completedAt), DAY(o.completedAt) " +
            "ORDER BY YEAR(o.completedAt) ASC, MONTH(o.completedAt) ASC, DAY(o.completedAt) ASC")
    List<Object[]> getRevenueGroupedByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT YEAR(o.completedAt), MONTH(o.completedAt), COALESCE(SUM(o.totalAmount), 0), COUNT(o.id) " +
            "FROM Order o " +
            "WHERE o.status = com.ecomart.entity.enums.OrderStatus.COMPLETED " +
            "AND o.completedAt >= :start AND o.completedAt <= :end " +
            "GROUP BY YEAR(o.completedAt), MONTH(o.completedAt) " +
            "ORDER BY YEAR(o.completedAt) ASC, MONTH(o.completedAt) ASC")
    List<Object[]> getRevenueGroupedByMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT YEAR(o.completedAt), COALESCE(SUM(o.totalAmount), 0), COUNT(o.id) " +
            "FROM Order o " +
            "WHERE o.status = com.ecomart.entity.enums.OrderStatus.COMPLETED " +
            "AND o.completedAt >= :start AND o.completedAt <= :end " +
            "GROUP BY YEAR(o.completedAt) " +
            "ORDER BY YEAR(o.completedAt) ASC")
    List<Object[]> getRevenueGroupedByYear(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT o.paymentMethod, COUNT(o.id), COALESCE(SUM(o.totalAmount), 0) " +
            "FROM Order o " +
            "WHERE o.status = com.ecomart.entity.enums.OrderStatus.COMPLETED " +
            "AND o.completedAt >= :start AND o.completedAt <= :end " +
            "GROUP BY o.paymentMethod")
    List<Object[]> getPaymentMethodDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT u.id, u.fullName, u.email, u.phoneNumber, COUNT(o.id), COALESCE(SUM(o.totalAmount), 0) " +
            "FROM Order o JOIN o.user u " +
            "WHERE o.status = com.ecomart.entity.enums.OrderStatus.COMPLETED " +
            "GROUP BY u.id, u.fullName, u.email, u.phoneNumber " +
            "ORDER BY SUM(o.totalAmount) DESC")
    List<Object[]> findTopCustomers(Pageable pageable);

    @Query("SELECT u.id, COUNT(o.id) " +
            "FROM Order o JOIN o.user u " +
            "WHERE o.status = com.ecomart.entity.enums.OrderStatus.COMPLETED " +
            "GROUP BY u.id")
    List<Object[]> countCompletedOrdersPerUser();

    @Query("SELECT u.id, u.fullName, u.email, u.phoneNumber, " +
            "COUNT(o.id), " +
            "SUM(CASE WHEN o.status = com.ecomart.entity.enums.OrderStatus.CANCELLED THEN 1 ELSE 0 END) " +
            "FROM Order o JOIN o.user u " +
            "GROUP BY u.id, u.fullName, u.email, u.phoneNumber " +
            "HAVING COUNT(o.id) >= 2")
    List<Object[]> findCustomerOrderAndCancelStats();
}
