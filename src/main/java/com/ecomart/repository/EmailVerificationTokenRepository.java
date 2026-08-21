package com.ecomart.repository;

import com.ecomart.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByEmailAndOtpCodeAndIsUsedFalse(String email, String otpCode);

    Optional<EmailVerificationToken> findFirstByEmailOrderByCreatedAtDesc(String email);

    Optional<EmailVerificationToken> findFirstByEmailAndIsUsedFalseOrderByCreatedAtDesc(String email);

    long countByEmailAndCreatedAtAfter(String email, LocalDateTime since);

    List<EmailVerificationToken> findByEmailAndIsUsedFalse(String email);

    void deleteByEmail(String email);
}
