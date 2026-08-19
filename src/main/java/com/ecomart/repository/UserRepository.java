package com.ecomart.repository;

import com.ecomart.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role.name = :roleName " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR u.phoneNumber LIKE CONCAT('%', :keyword, '%')) " +
           "AND (:isActive IS NULL OR u.isActive = :isActive)")
    Page<User> findCustomersForAdmin(@Param("roleName") String roleName,
                                     @Param("keyword") String keyword,
                                     @Param("isActive") Boolean isActive,
                                     Pageable pageable);
}

