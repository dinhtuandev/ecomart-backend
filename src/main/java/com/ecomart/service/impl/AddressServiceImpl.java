package com.ecomart.service.impl;

import com.ecomart.dto.request.AddressRequest;
import com.ecomart.dto.response.AddressResponse;
import com.ecomart.entity.Address;
import com.ecomart.entity.User;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.AddressRepository;
import com.ecomart.repository.UserRepository;
import com.ecomart.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream()
                .map(this::mapToAddressResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + userId));

        List<Address> existingAddresses = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
        boolean shouldBeDefault = existingAddresses.isEmpty() || Boolean.TRUE.equals(request.getIsDefault());

        if (shouldBeDefault && !existingAddresses.isEmpty()) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(oldDefault -> {
                oldDefault.setDefault(false);
                addressRepository.save(oldDefault);
            });
        }

        Address address = Address.builder()
                .user(user)
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .addressDetail(request.getAddressDetail())
                .ward(request.getWard())
                .district(request.getDistrict())
                .province(request.getProvince())
                .isDefault(shouldBeDefault)
                .build();

        Address savedAddress = addressRepository.save(address);
        return mapToAddressResponse(savedAddress);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại hoặc không thuộc về người dùng ID: " + userId));

        boolean requestIsDefault = Boolean.TRUE.equals(request.getIsDefault());

        if (requestIsDefault && !address.isDefault()) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(oldDefault -> {
                oldDefault.setDefault(false);
                addressRepository.save(oldDefault);
            });
            address.setDefault(true);
        } else if (request.getIsDefault() != null && !requestIsDefault && address.isDefault()) {
            address.setDefault(false);
        }

        address.setRecipientName(request.getRecipientName());
        address.setRecipientPhone(request.getRecipientPhone());
        address.setAddressDetail(request.getAddressDetail());
        address.setWard(request.getWard());
        address.setDistrict(request.getDistrict());
        address.setProvince(request.getProvince());

        Address updatedAddress = addressRepository.save(address);
        return mapToAddressResponse(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại hoặc không thuộc về người dùng ID: " + userId));

        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                Address newDefault = remaining.get(0);
                newDefault.setDefault(true);
                addressRepository.save(newDefault);
            }
        }
    }

    private AddressResponse mapToAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .addressDetail(address.getAddressDetail())
                .ward(address.getWard())
                .district(address.getDistrict())
                .province(address.getProvince())
                .isDefault(address.isDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}
