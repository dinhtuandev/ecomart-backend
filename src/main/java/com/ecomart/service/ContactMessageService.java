package com.ecomart.service;

import com.ecomart.dto.request.CreateContactMessageRequest;
import com.ecomart.dto.response.ContactMessageResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.entity.enums.ContactStatus;

import java.time.LocalDate;

public interface ContactMessageService {

    ContactMessageResponse createMessage(CreateContactMessageRequest request);

    PageResponse<ContactMessageResponse> getAdminMessages(
            int page,
            int pageSize,
            ContactStatus status,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate
    );

    ContactMessageResponse getAdminMessageDetail(Long messageId);

    ContactMessageResponse resolveMessage(Long messageId);
}
