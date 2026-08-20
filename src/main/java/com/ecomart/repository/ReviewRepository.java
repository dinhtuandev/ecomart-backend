package com.ecomart.repository;

import com.ecomart.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    boolean existsByOrderItemId(Long orderItemId);

    Optional<Review> findByOrderItemId(Long orderItemId);

    Page<Review> findByUserId(Long userId, Pageable pageable);

    Page<Review> findByProductIdAndIsVisibleTrue(Long productId, Pageable pageable);

    Page<Review> findByProductIdAndRatingAndIsVisibleTrue(Long productId, Integer rating, Pageable pageable);

    long countByProductIdAndIsVisibleTrue(Long productId);

    long countByProductIdAndRatingAndIsVisibleTrue(Long productId, Integer rating);

    @Query("SELECT AVG(CAST(r.rating as double)) FROM Review r WHERE r.product.id = :productId AND r.isVisible = true")
    Double getAverageRatingByProductId(@Param("productId") Long productId);
}
