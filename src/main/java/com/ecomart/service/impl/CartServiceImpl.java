package com.ecomart.service.impl;

import com.ecomart.dto.request.AddToCartRequest;
import com.ecomart.dto.request.UpdateCartItemRequest;
import com.ecomart.dto.response.CartItemResponse;
import com.ecomart.dto.response.CartResponse;
import com.ecomart.entity.*;
import com.ecomart.exception.ForbiddenException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.exception.UnprocessableEntityException;
import com.ecomart.repository.CartItemRepository;
import com.ecomart.repository.CartRepository;
import com.ecomart.repository.ProductRepository;
import com.ecomart.repository.UserRepository;
import com.ecomart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + request.getProductId()));

        if (!product.isVisible()) {
            throw new UnprocessableEntityException("Sản phẩm tạm thời không khả dụng để thêm vào giỏ hàng");
        }

        int availableStock = getAvailableStock(product);
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());

        int currentQtyInCart = existingItemOpt.map(CartItem::getQuantity).orElse(0);
        int targetQty = currentQtyInCart + request.getQuantity();

        if (targetQty > availableStock) {
            throw new UnprocessableEntityException("Số lượng sản phẩm trong giỏ hàng (" + targetQty + ") vượt quá tồn kho khả dụng (" + availableStock + ")");
        }

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(targetQty);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(targetQty)
                    .build();
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        Cart updatedCart = cartRepository.findById(cart.getId()).orElse(cart);
        return mapToCartResponse(updatedCart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Mục giỏ hàng không tồn tại với ID: " + cartItemId));

        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền thao tác trên mục giỏ hàng này");
        }

        Product product = cartItem.getProduct();
        if (!product.isVisible()) {
            throw new UnprocessableEntityException("Sản phẩm này hiện không còn khả dụng để cập nhật số lượng");
        }

        int availableStock = getAvailableStock(product);
        if (request.getQuantity() > availableStock) {
            throw new UnprocessableEntityException("Số lượng yêu cầu (" + request.getQuantity() + ") vượt quá tồn kho khả dụng (" + availableStock + ")");
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        Cart cart = cartRepository.findById(cartItem.getCart().getId()).orElse(cartItem.getCart());
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public void removeCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Mục giỏ hàng không tồn tại với ID: " + cartItemId));

        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền thao tác trên mục giỏ hàng này");
        }

        Cart cart = cartItem.getCart();
        cart.getItems().remove(cartItem);
        cartItemRepository.delete(cartItem);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteAllByCartId(cart.getId());
        cart.getItems().clear();
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + userId));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private int getAvailableStock(Product product) {
        return product.getInventory() != null ? product.getInventory().getQuantity() : 0;
    }

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;

        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                Product product = item.getProduct();
                int stock = getAvailableStock(product);
                boolean isAvailable = product.isVisible() && (stock >= item.getQuantity());

                BigDecimal subtotal = product.getSellingPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

                if (isAvailable) {
                    totalAmount = totalAmount.add(subtotal);
                }
                totalQuantity += item.getQuantity();

                String primaryImageUrl = getPrimaryImageUrl(product);

                itemResponses.add(CartItemResponse.builder()
                        .id(item.getId())
                        .productId(product.getId())
                        .productName(product.getName())
                        .productImageUrl(primaryImageUrl)
                        .sellingPrice(product.getSellingPrice())
                        .originalPrice(product.getOriginalPrice())
                        .quantity(item.getQuantity())
                        .subtotal(subtotal)
                        .quantityInStock(stock)
                        .isVisible(product.isVisible())
                        .isAvailable(isAvailable)
                        .build());
            }
        }

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .totalItems(itemResponses.size())
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private String getPrimaryImageUrl(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        return product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElseGet(() -> product.getImages().get(0).getImageUrl());
    }
}
