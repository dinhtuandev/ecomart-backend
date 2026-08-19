package com.ecomart.service.impl;

import com.ecomart.dto.request.CertificationRequest;
import com.ecomart.dto.response.CertificationResponse;
import com.ecomart.entity.Certification;
import com.ecomart.exception.BadRequestException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.CertificationRepository;
import com.ecomart.service.CertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificationServiceImpl implements CertificationService {

    private final CertificationRepository certificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CertificationResponse> getActiveCertifications() {
        return certificationRepository.findAllByIsActiveTrueOrderByNameAsc().stream()
                .map(this::mapToCertificationResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificationResponse> getAllCertificationsForAdmin() {
        return certificationRepository.findAllByOrderByNameAsc().stream()
                .map(this::mapToCertificationResponse)
                .toList();
    }

    @Override
    @Transactional
    public CertificationResponse createCertification(CertificationRequest request) {
        if (certificationRepository.existsByName(request.getName())) {
            throw new BadRequestException("Tên chứng nhận '" + request.getName() + "' đã tồn tại");
        }

        Certification certification = Certification.builder()
                .name(request.getName())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();

        Certification savedCertification = certificationRepository.save(certification);
        return mapToCertificationResponse(savedCertification);
    }

    @Override
    @Transactional
    public CertificationResponse updateCertification(Long certificationId, CertificationRequest request) {
        Certification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Chứng nhận không tồn tại với ID: " + certificationId));

        if (request.getName() != null && certificationRepository.existsByNameAndIdNot(request.getName(), certificationId)) {
            throw new BadRequestException("Tên chứng nhận '" + request.getName() + "' đã được sử dụng bởi chứng nhận khác");
        }

        if (request.getName() != null) {
            certification.setName(request.getName());
        }
        if (request.getDescription() != null) {
            certification.setDescription(request.getDescription());
        }
        if (request.getIconUrl() != null) {
            certification.setIconUrl(request.getIconUrl());
        }
        if (request.getIsActive() != null) {
            certification.setActive(request.getIsActive());
        }

        Certification updatedCertification = certificationRepository.save(certification);
        return mapToCertificationResponse(updatedCertification);
    }

    private CertificationResponse mapToCertificationResponse(Certification certification) {
        return CertificationResponse.builder()
                .id(certification.getId())
                .name(certification.getName())
                .description(certification.getDescription())
                .iconUrl(certification.getIconUrl())
                .isActive(certification.isActive())
                .createdAt(certification.getCreatedAt())
                .updatedAt(certification.getUpdatedAt())
                .build();
    }
}
