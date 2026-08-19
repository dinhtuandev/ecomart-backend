package com.ecomart.controller;

import com.ecomart.dto.request.UpdateInventoryRequest;
import com.ecomart.dto.response.ApiResponse;
import com.ecomart.dto.response.InventoryResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getAdminInventory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean lowStockOnly
    ) {
        PageResponse<InventoryResponse> response = inventoryService.getAdminInventory(page, pageSize, keyword, lowStockOnly);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tồn kho thành công.", response));
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateStock(@PathVariable Long productId,
                                                                      @Valid @RequestBody UpdateInventoryRequest request) {
        InventoryResponse response = inventoryService.updateStock(productId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật số lượng tồn kho thành công.", response));
    }
}
