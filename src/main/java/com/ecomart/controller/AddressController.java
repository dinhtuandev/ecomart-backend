package com.ecomart.controller;

import com.ecomart.dto.request.AddressRequest;
import com.ecomart.dto.response.AddressResponse;
import com.ecomart.dto.response.ApiResponse;
import com.ecomart.security.UserPrincipal;
import com.ecomart.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<AddressResponse> response = addressService.getUserAddresses(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách địa chỉ giao hàng thành công.", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(@AuthenticationPrincipal UserPrincipal currentUser,
                                                                       @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.createAddress(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo địa chỉ giao hàng mới thành công.", response));
    }

    @PatchMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(@AuthenticationPrincipal UserPrincipal currentUser,
                                                                       @PathVariable Long addressId,
                                                                       @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.updateAddress(currentUser.getId(), addressId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật địa chỉ giao hàng thành công.", response));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal UserPrincipal currentUser,
                                              @PathVariable Long addressId) {
        addressService.deleteAddress(currentUser.getId(), addressId);
        return ResponseEntity.noContent().build();
    }
}
