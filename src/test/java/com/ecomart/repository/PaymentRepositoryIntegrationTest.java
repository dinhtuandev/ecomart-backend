package com.ecomart.repository;

import com.ecomart.entity.Order;
import com.ecomart.entity.PaymentTransaction;
import com.ecomart.entity.Role;
import com.ecomart.entity.User;
import com.ecomart.entity.enums.OrderStatus;
import com.ecomart.entity.enums.PaymentGateway;
import com.ecomart.entity.enums.PaymentMethod;
import com.ecomart.entity.enums.PaymentStatus;
import com.ecomart.entity.enums.PaymentTransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class PaymentRepositoryIntegrationTest {

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("Ném DataIntegrityViolationException khi lưu 2 transaction có cùng (gateway, gateway_transaction_no)")
    void savePaymentTransaction_ThrowsDataIntegrityViolationException_WhenDuplicateGatewayAndReferenceCode() {
        Role role = Role.builder().name("CUSTOMER").description("Customer Role").build();
        Role savedRole = roleRepository.save(role);

        User user = User.builder()
                .email("test_payment@example.com")
                .passwordHash("hashed_password")
                .fullName("Test Payment")
                .role(savedRole)
                .build();
        User savedUser = userRepository.save(user);

        Order order = Order.builder()
                .user(savedUser)
                .orderCode("EM-20260820-TEST1")
                .status(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.SEPAY)
                .paymentStatus(PaymentStatus.UNPAID)
                .totalAmount(BigDecimal.valueOf(200000))
                .recipientName("Test")
                .recipientPhone("0901234567")
                .deliveryAddress("HCM")
                .orderedAt(LocalDateTime.now())
                .build();
        Order savedOrder = orderRepository.save(order);

        PaymentTransaction tx1 = PaymentTransaction.builder()
                .order(savedOrder)
                .paymentRef("EM-20260820-TEST1-P111111111111")
                .gateway(PaymentGateway.SEPAY)
                .amount(BigDecimal.valueOf(200000))
                .gatewayTransactionNo("REF_123456")
                .status(PaymentTransactionStatus.SUCCESS)
                .build();
        paymentTransactionRepository.saveAndFlush(tx1);

        PaymentTransaction tx2 = PaymentTransaction.builder()
                .order(savedOrder)
                .paymentRef("EM-20260820-TEST1-P222222222222")
                .gateway(PaymentGateway.SEPAY)
                .amount(BigDecimal.valueOf(200000))
                .gatewayTransactionNo("REF_123456") // Duplicate (SEPAY, REF_123456)
                .status(PaymentTransactionStatus.SUCCESS)
                .build();

        assertThatThrownBy(() -> paymentTransactionRepository.saveAndFlush(tx2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
