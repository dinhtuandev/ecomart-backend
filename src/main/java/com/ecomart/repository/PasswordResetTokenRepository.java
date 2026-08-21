package com.ecomart.repository;

import com.ecomart.entity.PasswordResetToken;
import com.ecomart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findFirstByUserOrderByCreatedAtDesc(User user);

    Optional<PasswordResetToken> findFirstByUserAndIsUsedFalseOrderByCreatedAtDesc(User user);

    long countByUserAndCreatedAtAfter(User user, LocalDateTime since);

    List<PasswordResetToken> findByUserAndIsUsedFalse(User user);
}
