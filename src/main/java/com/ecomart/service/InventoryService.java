package com.ecomart.service;

import com.ecomart.dto.request.UpdateInventoryRequest;
import com.ecomart.dto.response.InventoryResponse;
import com.ecomart.dto.response.PageResponse;

public interface InventoryService {

    PageResponse<InventoryResponse> getAdminInventory(int page, int pageSize, String keyword, Boolean lowStockOnly);

    InventoryResponse updateStock(Long productId, UpdateInventoryRequest request);
}
