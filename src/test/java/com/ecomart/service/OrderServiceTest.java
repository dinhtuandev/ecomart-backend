package com.ecomart.service;

import com.ecomart.dto.request.AdminCancelOrderRequest;
import com.ecomart.dto.request.CreateOrderRequest;
import com.ecomart.dto.request.UpdatePaymentStatusRequest;
import com.ecomart.dto.response.CreateOrderResponse;
import com.ecomart.dto.response.OrderResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.entity.*;
import com.ecomart.entity.enums.OrderStatus;
import com.ecomart.entity.enums.PaymentMethod;
import com.ecomart.entity.enums.PaymentStatus;
import com.ecomart.exception.ConflictException;
import com.ecomart.exception.ForbiddenException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.exception.UnprocessableEntityException;
import com.ecomart.repository.*;
import com.ecomart.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Address address;
    private Product product;
    private Inventory inventory;
    private Cart cart;
    private CartItem cartItem;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("customer@example.com").fullName("Nguyen Van A").build();

        address = Address.builder()
                .id(10L)
                .user(user)
                .recipientName("Nguyen Van A")
                .recipientPhone("0901234567")
                .addressDetail("123 Street")
                .ward("Ward 1")
                .district("District 1")
                .province("TP. HCM")
                .build();

        product = Product.builder()
                .id(100L)
                .name("Bình giữ nhiệt Eco")
                .sellingPrice(BigDecimal.valueOf(200000))
                .isVisible(true)
                .build();

        inventory = Inventory.builder().id(1L).product(product).quantity(10).build();

        cartItem = CartItem.builder().id(1L).product(product).quantity(2).build();
        cart = Cart.builder().id(1L).user(user).items(new ArrayList<>(List.of(cartItem))).build();
        cartItem.setCart(cart);

        orderItem = OrderItem.builder()
                .id(1L)
                .product(product)
                .productName("Bình giữ nhiệt Eco")
                .unitPrice(BigDecimal.valueOf(200000))
                .quantity(2)
                .lineTotal(BigDecimal.valueOf(400000))
                .build();

        order = Order.builder()
                .id(1000L)
                .user(user)
                .orderCode("EM-20260820-0001")
                .status(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.UNPAID)
                .totalAmount(BigDecimal.valueOf(400000))
                .recipientName("Nguyen Van A")
                .recipientPhone("0901234567")
                .deliveryAddress("123 Street, Ward 1, District 1, TP. HCM")
                .items(new ArrayList<>(List.of(orderItem)))
                .paymentTransactions(new ArrayList<>())
                .build();
        orderItem.setOrder(order);
    }

    @Test
    @DisplayName("Đặt hàng COD thành công: trừ tồn kho, xóa giỏ hàng, paymentUrl = null")
    void createOrder_Success_COD() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .addressId(10L)
                .paymentMethod(PaymentMethod.COD)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        CreateOrderResponse response = orderService.createOrder(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getOrder()).isNotNull();
        assertThat(response.getPaymentUrl()).isNull();
        assertThat(inventory.getQuantity()).isEqualTo(8); // 10 - 2 = 8
        verify(inventoryRepository, times(1)).save(inventory);
        verify(cartItemRepository, times(1)).deleteAllByCartId(1L);
    }

    @Test
    @DisplayName("Đặt hàng VNPay thành công: tạo PaymentTransaction và sinh paymentUrl")
    void createOrder_Success_VNPAY() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .addressId(10L)
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        order.setPaymentMethod(PaymentMethod.VNPAY);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenReturn(PaymentTransaction.builder().build());

        CreateOrderResponse response = orderService.createOrder(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentUrl()).isNotNull();
        assertThat(response.getPaymentUrl()).contains("sandbox.vnpayment.vn");
        verify(paymentTransactionRepository, times(1)).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("Ném 422 khi đặt hàng với giỏ hàng rỗng")
    void createOrder_Throws422_WhenCartIsEmpty() {
        cart.getItems().clear();
        CreateOrderRequest request = CreateOrderRequest.builder().addressId(10L).paymentMethod(PaymentMethod.COD).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Giỏ hàng đang trống");
    }

    @Test
    @DisplayName("Ném 422 khi đặt hàng nhưng sản phẩm không đủ tồn kho")
    void createOrder_Throws422_WhenInsufficientStock() {
        inventory.setQuantity(1); // Yêu cầu 2 nhưng chỉ còn 1
        CreateOrderRequest request = CreateOrderRequest.builder().addressId(10L).paymentMethod(PaymentMethod.COD).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("không đủ tồn kho");
    }

    @Test
    @DisplayName("Ném 403 khi đặt hàng bằng địa chỉ của người khác")
    void createOrder_Throws403_WhenAddressBelongsToOtherUser() {
        User otherUser = User.builder().id(2L).build();
        address.setUser(otherUser);
        CreateOrderRequest request = CreateOrderRequest.builder().addressId(10L).paymentMethod(PaymentMethod.COD).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));

        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("không thuộc quyền sở hữu");
    }

    @Test
    @DisplayName("Khách hàng hủy đơn PENDING thành công: hoàn tồn kho")
    void cancelCustomerOrder_Success_RestoresStock() {
        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.cancelCustomerOrder(1L, 1000L);

        assertThat(response).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationReason()).isEqualTo("Khách hàng hủy đơn");
        assertThat(inventory.getQuantity()).isEqualTo(12); // 10 + 2 = 12
        verify(inventoryRepository, times(1)).save(inventory);
    }

    @Test
    @DisplayName("Ném 409 khi khách hàng hủy đơn đã CONFIRMED")
    void cancelCustomerOrder_Throws409_WhenNotPending() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelCustomerOrder(1L, 1000L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Chỉ có thể hủy đơn hàng khi đang ở trạng thái Chờ xác nhận");
    }

    @Test
    @DisplayName("Admin xác nhận đơn COD PENDING thành công")
    void confirmOrder_Success_COD() {
        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.confirmOrder(1000L);

        assertThat(response).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("Ném 409 khi Admin xác nhận đơn online chưa thanh toán")
    void confirmOrder_Throws409_WhenOnlineUnpaid() {
        order.setPaymentMethod(PaymentMethod.VNPAY);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirmOrder(1000L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("chưa thanh toán (PAID), không thể xác nhận");
    }

    @Test
    @DisplayName("Admin hủy đơn kèm lý do thành công: hoàn tồn kho")
    void cancelAdminOrder_Success_RestoresStock() {
        AdminCancelOrderRequest request = AdminCancelOrderRequest.builder().cancellationReason("Hết hàng đột xuất").build();

        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.cancelAdminOrder(1000L, request);

        assertThat(response).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationReason()).isEqualTo("Hết hàng đột xuất");
        assertThat(inventory.getQuantity()).isEqualTo(12);
        verify(inventoryRepository, times(1)).save(inventory);
    }

    @Test
    @DisplayName("Admin hoàn thành đơn CONFIRMED COD: tự động gán paymentStatus = PAID")
    void completeOrder_Success_SetsCODAsPaid() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.completeOrder(1000L);

        assertThat(response).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("Admin cập nhật trạng thái REFUNDED cho đơn online CANCELLED đã thanh toán thành công")
    void updatePaymentStatus_Success_Refunded() {
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentMethod(PaymentMethod.VNPAY);
        order.setPaymentStatus(PaymentStatus.PAID);

        UpdatePaymentStatusRequest request = UpdatePaymentStatusRequest.builder().paymentStatus(PaymentStatus.REFUNDED).build();

        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.updatePaymentStatus(1000L, request);

        assertThat(response).isNotNull();
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }
}
