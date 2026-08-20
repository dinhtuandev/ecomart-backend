package com.ecomart.entity;

import com.ecomart.entity.enums.PaymentGateway;
import com.ecomart.entity.enums.PaymentTransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_transactions",
        indexes = {
                @Index(name = "idx_payment_tx_order_id", columnList = "order_id"),
                @Index(name = "idx_payment_tx_payment_ref", columnList = "payment_ref"),
                @Index(name = "idx_payment_tx_gateway_no", columnList = "gateway_transaction_no")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_tx_gateway_no", columnNames = {"gateway", "gateway_transaction_no"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "payment_ref", nullable = false, unique = true, length = 64)
    private String paymentRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway", nullable = false, length = 20)
    private PaymentGateway gateway;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "gateway_transaction_no", length = 100)
    private String gatewayTransactionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentTransactionStatus status;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
