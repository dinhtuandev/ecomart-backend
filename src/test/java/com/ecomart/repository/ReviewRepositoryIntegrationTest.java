package com.ecomart.repository;

import com.ecomart.entity.*;
import com.ecomart.entity.enums.OrderStatus;
import com.ecomart.entity.enums.PaymentMethod;
import com.ecomart.entity.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class ReviewRepositoryIntegrationTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    @DisplayName("Ném DataIntegrityViolationException khi lưu 2 review cho cùng một order_item_id")
    void saveReview_ThrowsDataIntegrityViolationException_WhenDuplicateOrderItemId() {
        Role role = roleRepository.save(Role.builder().name("CUSTOMER").description("Role").build());
        User user = userRepository.save(User.builder().email("review_user@example.com").passwordHash("hash").fullName("Review User").role(role).build());

        Category category = categoryRepository.save(Category.builder().name("Category 1").description("cat-1").build());
        Brand brand = brandRepository.save(Brand.builder().name("Brand 1").description("brand-1").build());
        Product product = productRepository.save(Product.builder().name("Product 1").category(category).brand(brand).sellingPrice(BigDecimal.valueOf(100000)).build());

        Order order = orderRepository.save(Order.builder()
                .user(user)
                .orderCode("EM-20260820-RV001")
                .status(OrderStatus.COMPLETED)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PAID)
                .totalAmount(BigDecimal.valueOf(100000))
                .recipientName("User")
                .recipientPhone("0901234567")
                .deliveryAddress("HCM")
                .orderedAt(LocalDateTime.now())
                .build());

        OrderItem orderItem = orderItemRepository.save(OrderItem.builder()
                .order(order)
                .product(product)
                .productName("Product 1")
                .unitPrice(BigDecimal.valueOf(100000))
                .quantity(1)
                .lineTotal(BigDecimal.valueOf(100000))
                .build());

        Review r1 = Review.builder()
                .user(user)
                .product(product)
                .orderItem(orderItem)
                .rating(5)
                .comment("Tuyệt vời")
                .isVisible(true)
                .build();
        reviewRepository.saveAndFlush(r1);

        Review r2 = Review.builder()
                .user(user)
                .product(product)
                .orderItem(orderItem) // Duplicate orderItem
                .rating(4)
                .comment("Khá ổn")
                .isVisible(true)
                .build();

        assertThatThrownBy(() -> reviewRepository.saveAndFlush(r2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
