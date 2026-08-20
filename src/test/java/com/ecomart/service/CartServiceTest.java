package com.ecomart.service;

import com.ecomart.dto.request.AddToCartRequest;
import com.ecomart.dto.request.UpdateCartItemRequest;
import com.ecomart.dto.response.CartResponse;
import com.ecomart.entity.*;
import com.ecomart.exception.ForbiddenException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.exception.UnprocessableEntityException;
import com.ecomart.repository.CartItemRepository;
import com.ecomart.repository.CartRepository;
import com.ecomart.repository.ProductRepository;
import com.ecomart.repository.UserRepository;
import com.ecomart.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;
    private Cart cart;
    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("user@example.com").build();
        cart = Cart.builder().id(10L).user(user).items(new ArrayList<>()).build();

        product = Product.builder()
                .id(100L)
                .name("Bình nước tre")
                .sellingPrice(BigDecimal.valueOf(150000))
                .originalPrice(BigDecimal.valueOf(180000))
                .isVisible(true)
                .build();

        inventory = Inventory.builder().id(1L).product(product).quantity(20).build();
        product.setInventory(inventory);
    }

    @Test
    @DisplayName("Tự động tạo giỏ hàng rỗng khi người dùng chưa có giỏ")
    void getCart_AutoInitializes_WhenCartDoesNotExist() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.getCart(1L);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalAmount()).isEqualTo(BigDecimal.ZERO);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Thêm sản phẩm mới vào giỏ hàng thành công")
    void addToCart_Success_WhenNewProduct() {
        AddToCartRequest request = AddToCartRequest.builder().productId(100L).quantity(2).build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(10L, 100L)).thenReturn(Optional.empty());
        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.addToCart(1L, request);

        assertThat(response).isNotNull();
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Cộng dồn số lượng khi sản phẩm đã tồn tại trong giỏ hàng")
    void addToCart_Success_CumulativeQuantity_WhenProductAlreadyInCart() {
        AddToCartRequest request = AddToCartRequest.builder().productId(100L).quantity(3).build();
        CartItem existingItem = CartItem.builder().id(1L).cart(cart).product(product).quantity(2).build();
        cart.getItems().add(existingItem);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(10L, 100L)).thenReturn(Optional.of(existingItem));
        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.addToCart(1L, request);

        assertThat(response).isNotNull();
        assertThat(existingItem.getQuantity()).isEqualTo(5);
        verify(cartItemRepository, times(1)).save(existingItem);
    }

    @Test
    @DisplayName("Ném 422 khi số lượng cộng dồn vượt quá tồn kho khả dụng")
    void addToCart_Throws422_WhenCumulativeQuantityExceedsStock() {
        AddToCartRequest request = AddToCartRequest.builder().productId(100L).quantity(15).build();
        CartItem existingItem = CartItem.builder().id(1L).cart(cart).product(product).quantity(10).build();
        cart.getItems().add(existingItem);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(10L, 100L)).thenReturn(Optional.of(existingItem));

        assertThatThrownBy(() -> cartService.addToCart(1L, request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("vượt quá tồn kho");
    }

    @Test
    @DisplayName("Ném 422 khi thêm sản phẩm đang bị ẩn vào giỏ")
    void addToCart_Throws422_WhenProductIsHidden() {
        product.setVisible(false);
        AddToCartRequest request = AddToCartRequest.builder().productId(100L).quantity(1).build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addToCart(1L, request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("không khả dụng");
    }

    @Test
    @DisplayName("Ném 404 khi thêm sản phẩm không tồn tại")
    void addToCart_Throws404_WhenProductDoesNotExist() {
        AddToCartRequest request = AddToCartRequest.builder().productId(999L).quantity(1).build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("không tồn tại");
    }

    @Test
    @DisplayName("Cập nhật số lượng món hàng thành công")
    void updateCartItem_Success() {
        CartItem cartItem = CartItem.builder().id(5L).cart(cart).product(product).quantity(2).build();
        UpdateCartItemRequest request = UpdateCartItemRequest.builder().quantity(5).build();

        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(cartItem));
        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.updateCartItem(1L, 5L, request);

        assertThat(response).isNotNull();
        assertThat(cartItem.getQuantity()).isEqualTo(5);
        verify(cartItemRepository, times(1)).save(cartItem);
    }

    @Test
    @DisplayName("Ném 403 Forbidden khi thao tác trên mục giỏ hàng của user khác (IDOR)")
    void updateCartItem_Throws403_WhenIDOR_NotOwner() {
        User otherUser = User.builder().id(2L).email("other@example.com").build();
        Cart otherCart = Cart.builder().id(20L).user(otherUser).build();
        CartItem cartItem = CartItem.builder().id(5L).cart(otherCart).product(product).quantity(2).build();

        UpdateCartItemRequest request = UpdateCartItemRequest.builder().quantity(3).build();

        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(cartItem));

        assertThatThrownBy(() -> cartService.updateCartItem(1L, 5L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("không có quyền");
    }

    @Test
    @DisplayName("Ném 404 Not Found khi mục giỏ hàng không tồn tại trong DB")
    void updateCartItem_Throws404_WhenCartItemNotFound() {
        UpdateCartItemRequest request = UpdateCartItemRequest.builder().quantity(3).build();

        when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateCartItem(1L, 999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("không tồn tại");
    }

    @Test
    @DisplayName("Ném 422 khi số lượng cập nhật vượt quá tồn kho")
    void updateCartItem_Throws422_WhenQuantityExceedsStock() {
        CartItem cartItem = CartItem.builder().id(5L).cart(cart).product(product).quantity(2).build();
        UpdateCartItemRequest request = UpdateCartItemRequest.builder().quantity(50).build();

        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(cartItem));

        assertThatThrownBy(() -> cartService.updateCartItem(1L, 5L, request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("vượt quá tồn kho");
    }

    @Test
    @DisplayName("Xóa 1 món hàng khỏi giỏ thành công")
    void removeCartItem_Success() {
        CartItem cartItem = CartItem.builder().id(5L).cart(cart).product(product).quantity(2).build();
        cart.getItems().add(cartItem);

        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(cartItem));

        cartService.removeCartItem(1L, 5L);

        verify(cartItemRepository, times(1)).delete(cartItem);
        assertThat(cart.getItems()).doesNotContain(cartItem);
    }

    @Test
    @DisplayName("Ném 403 Forbidden khi xóa món hàng của user khác")
    void removeCartItem_Throws403_WhenNotOwner() {
        User otherUser = User.builder().id(2L).build();
        Cart otherCart = Cart.builder().id(20L).user(otherUser).build();
        CartItem cartItem = CartItem.builder().id(5L).cart(otherCart).product(product).quantity(2).build();

        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(cartItem));

        assertThatThrownBy(() -> cartService.removeCartItem(1L, 5L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Xóa sạch toàn bộ giỏ hàng thành công")
    void clearCart_Success() {
        CartItem cartItem = CartItem.builder().id(5L).cart(cart).product(product).quantity(2).build();
        cart.getItems().add(cartItem);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        cartService.clearCart(1L);

        verify(cartItemRepository, times(1)).deleteAllByCartId(10L);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Đánh dấu isAvailable = false và loại khỏi totalAmount khi sản phẩm bị ẩn hoặc hết hàng")
    void getCart_CorrectlyFlagsHiddenOrOutOfStockItems() {
        product.setVisible(false); // Hidden product
        CartItem cartItem = CartItem.builder().id(1L).cart(cart).product(product).quantity(2).build();
        cart.getItems().add(cartItem);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(1L);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).isAvailable()).isFalse();
        assertThat(response.getItems().get(0).isVisible()).isFalse();
        assertThat(response.getTotalAmount()).isEqualTo(BigDecimal.ZERO);
    }
}
