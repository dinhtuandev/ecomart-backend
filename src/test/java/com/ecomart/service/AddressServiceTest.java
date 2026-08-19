package com.ecomart.service;

import com.ecomart.dto.request.AddressRequest;
import com.ecomart.dto.response.AddressResponse;
import com.ecomart.entity.Address;
import com.ecomart.entity.User;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.AddressRepository;
import com.ecomart.repository.UserRepository;
import com.ecomart.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private User user;
    private Address address1;
    private Address address2;

    @BeforeEach
    void setUp() {
        user = User.builder().id(10L).email("user@example.com").build();

        address1 = Address.builder()
                .id(1L)
                .user(user)
                .recipientName("Nguyen Van A")
                .recipientPhone("0901234567")
                .addressDetail("123 Street A")
                .ward("Ward 1")
                .district("District 1")
                .province("HCMC")
                .isDefault(true)
                .build();

        address2 = Address.builder()
                .id(2L)
                .user(user)
                .recipientName("Nguyen Van B")
                .recipientPhone("0907654321")
                .addressDetail("456 Street B")
                .ward("Ward 2")
                .district("District 2")
                .province("HCMC")
                .isDefault(false)
                .build();
    }

    @Test
    @DisplayName("getUserAddresses - Trả về danh sách địa chỉ thành công")
    void getUserAddresses_Success() {
        when(addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(10L))
                .thenReturn(List.of(address1, address2));

        List<AddressResponse> result = addressService.getUserAddresses(10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isDefault()).isTrue();
        assertThat(result.get(1).isDefault()).isFalse();
    }

    @Test
    @DisplayName("createAddress - Địa chỉ đầu tiên tự động thành địa chỉ mặc định")
    void createAddress_FirstAddress_AutoDefault() {
        AddressRequest request = AddressRequest.builder()
                .recipientName("Nguyen Van A")
                .recipientPhone("0901234567")
                .addressDetail("123 Street A")
                .isDefault(false)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(10L)).thenReturn(Collections.emptyList());
        when(addressRepository.save(any(Address.class))).thenAnswer(i -> {
            Address a = i.getArgument(0);
            a.setId(1L);
            return a;
        });

        AddressResponse response = addressService.createAddress(10L, request);

        assertThat(response.isDefault()).isTrue();
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    @DisplayName("createAddress - Khi cờ isDefault=true thì chuyển địa chỉ cũ về isDefault=false")
    void createAddress_WithDefaultTrue_UnsetsPreviousDefault() {
        AddressRequest request = AddressRequest.builder()
                .recipientName("Nguyen Van B")
                .recipientPhone("0907654321")
                .addressDetail("456 Street B")
                .isDefault(true)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(10L)).thenReturn(List.of(address1));
        when(addressRepository.findByUserIdAndIsDefaultTrue(10L)).thenReturn(Optional.of(address1));
        when(addressRepository.save(any(Address.class))).thenAnswer(i -> i.getArgument(0));

        AddressResponse response = addressService.createAddress(10L, request);

        assertThat(response.isDefault()).isTrue();
        assertThat(address1.isDefault()).isFalse();
        verify(addressRepository, times(2)).save(any(Address.class));
    }

    @Test
    @DisplayName("updateAddress - Thất bại khi địa chỉ không thuộc về user")
    void updateAddress_NotOwned_ThrowsException() {
        AddressRequest request = AddressRequest.builder()
                .recipientName("Hack Name")
                .recipientPhone("0900000000")
                .addressDetail("Fake Detail")
                .build();

        when(addressRepository.findByIdAndUserId(99L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.updateAddress(10L, 99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Địa chỉ không tồn tại hoặc không thuộc về người dùng");
    }

    @Test
    @DisplayName("deleteAddress - Xóa địa chỉ mặc định thì tự động chọn địa chỉ khác làm mặc định")
    void deleteAddress_DeleteDefault_SetsNewDefault() {
        when(addressRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(address1));
        when(addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(10L))
                .thenReturn(new ArrayList<>(List.of(address2)));

        addressService.deleteAddress(10L, 1L);

        verify(addressRepository).delete(address1);
        assertThat(address2.isDefault()).isTrue();
        verify(addressRepository).save(address2);
    }
}
