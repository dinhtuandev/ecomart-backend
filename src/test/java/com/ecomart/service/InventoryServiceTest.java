package com.ecomart.service;

import com.ecomart.dto.request.UpdateInventoryRequest;
import com.ecomart.dto.response.InventoryResponse;

import com.ecomart.entity.Inventory;
import com.ecomart.entity.Product;
import com.ecomart.repository.InventoryRepository;
import com.ecomart.repository.ProductRepository;
import com.ecomart.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        product = Product.builder().id(1L).name("Bình nước inox").build();
        inventory = Inventory.builder().id(1L).product(product).quantity(10).build();
    }

    @Test
    @DisplayName("Cập nhật số lượng tồn kho thành công")
    void updateStock_Success() {
        UpdateInventoryRequest request = UpdateInventoryRequest.builder().quantityInStock(25).build();

        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(Inventory.builder().id(1L).product(product).quantity(25).build());

        InventoryResponse response = inventoryService.updateStock(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getQuantityInStock()).isEqualTo(25);
        verify(inventoryRepository, times(1)).save(inventory);
    }
}
