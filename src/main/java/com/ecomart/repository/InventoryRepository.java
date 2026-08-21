package com.ecomart.repository;

import com.ecomart.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    Optional<Inventory> findByProductId(Long productId);

    long countByQuantityLessThanEqual(Integer threshold);

    @Query("SELECT i.product.id, i.product.name, i.product.category.name, i.quantity, i.product.sellingPrice " +
           "FROM Inventory i " +
           "WHERE i.quantity <= :threshold " +
           "ORDER BY i.quantity ASC")
    List<Object[]> findLowStockProductsDetailed(@Param("threshold") Integer threshold);
}
