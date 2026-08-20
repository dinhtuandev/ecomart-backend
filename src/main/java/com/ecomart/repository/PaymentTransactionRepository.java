package com.ecomart.repository;

import com.ecomart.entity.PaymentTransaction;
import com.ecomart.entity.enums.PaymentGateway;
import com.ecomart.entity.enums.PaymentTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByOrderId(Long orderId);

    Optional<PaymentTransaction> findByPaymentRef(String paymentRef);

    Optional<PaymentTransaction> findFirstByOrderIdAndGatewayAndStatusOrderByCreatedAtDesc(
            Long orderId,
            PaymentGateway gateway,
            PaymentTransactionStatus status
    );

    boolean existsByGatewayAndGatewayTransactionNo(PaymentGateway gateway, String gatewayTransactionNo);
}
