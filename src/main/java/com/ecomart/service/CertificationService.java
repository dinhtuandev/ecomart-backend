package com.ecomart.service;

import com.ecomart.dto.request.CertificationRequest;
import com.ecomart.dto.response.CertificationResponse;

import java.util.List;

public interface CertificationService {

    List<CertificationResponse> getActiveCertifications();

    List<CertificationResponse> getAllCertificationsForAdmin();

    CertificationResponse createCertification(CertificationRequest request);

    CertificationResponse updateCertification(Long certificationId, CertificationRequest request);
}
