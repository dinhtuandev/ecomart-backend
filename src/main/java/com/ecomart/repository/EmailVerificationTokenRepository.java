package com.ecomart.repository;

import com.ecomart.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByEmailAndOtpCodeAndIsUsedFalse(String email, String otpCode);

    List<EmailVerificationToken> findByEmailAndIsUsedFalse(String email);

    void deleteByEmail(String email);
}
