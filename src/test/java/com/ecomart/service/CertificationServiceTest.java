package com.ecomart.service;

import com.ecomart.dto.request.CertificationRequest;
import com.ecomart.dto.response.CertificationResponse;
import com.ecomart.entity.Certification;
import com.ecomart.exception.BadRequestException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.CertificationRepository;
import com.ecomart.service.impl.CertificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificationServiceTest {

    @Mock
    private CertificationRepository certificationRepository;

    @InjectMocks
    private CertificationServiceImpl certificationService;

    private Certification activeCertification;

    @BeforeEach
    void setUp() {
        activeCertification = Certification.builder()
                .id(1L)
                .name("USDA Organic")
                .description("Chứng nhận hữu cơ Hoa Kỳ")
                .iconUrl("https://example.com/usda.png")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Lấy danh sách chứng nhận active thành công")
    void getActiveCertifications_Success() {
        when(certificationRepository.findAllByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(activeCertification));

        List<CertificationResponse> responses = certificationService.getActiveCertifications();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("USDA Organic");
    }

    @Test
    @DisplayName("Tạo chứng nhận mới thành công khi tên chưa tồn tại")
    void createCertification_Success() {
        CertificationRequest request = CertificationRequest.builder()
                .name("Fair Trade")
                .description("Thương mại công bằng")
                .iconUrl("https://example.com/fairtrade.png")
                .isActive(true)
                .build();

        when(certificationRepository.existsByName("Fair Trade")).thenReturn(false);
        when(certificationRepository.save(any(Certification.class))).thenReturn(Certification.builder()
                .id(2L)
                .name("Fair Trade")
                .description("Thương mại công bằng")
                .iconUrl("https://example.com/fairtrade.png")
                .isActive(true)
                .build());

        CertificationResponse response = certificationService.createCertification(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Fair Trade");
        verify(certificationRepository, times(1)).save(any(Certification.class));
    }

    @Test
    @DisplayName("Tạo chứng nhận thất bại khi trùng tên")
    void createCertification_DuplicateName_ThrowsBadRequestException() {
        CertificationRequest request = CertificationRequest.builder()
                .name("USDA Organic")
                .build();

        when(certificationRepository.existsByName("USDA Organic")).thenReturn(true);

        assertThatThrownBy(() -> certificationService.createCertification(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    @DisplayName("Cập nhật chứng nhận thành công")
    void updateCertification_Success() {
        CertificationRequest request = CertificationRequest.builder()
                .name("USDA Organic Updated")
                .isActive(false)
                .build();

        when(certificationRepository.findById(1L)).thenReturn(Optional.of(activeCertification));
        when(certificationRepository.existsByNameAndIdNot("USDA Organic Updated", 1L)).thenReturn(false);
        when(certificationRepository.save(any(Certification.class))).thenReturn(activeCertification);

        CertificationResponse response = certificationService.updateCertification(1L, request);

        assertThat(response).isNotNull();
        verify(certificationRepository, times(1)).save(activeCertification);
    }

    @Test
    @DisplayName("Cập nhật chứng nhận thất bại khi không tìm thấy ID")
    void updateCertification_NotFound_ThrowsException() {
        CertificationRequest request = CertificationRequest.builder().name("Test").build();

        when(certificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificationService.updateCertification(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
