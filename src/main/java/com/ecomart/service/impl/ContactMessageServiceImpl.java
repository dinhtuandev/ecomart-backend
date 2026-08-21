package com.ecomart.service.impl;

import com.ecomart.dto.request.CreateContactMessageRequest;
import com.ecomart.dto.response.ContactMessageResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.entity.ContactMessage;
import com.ecomart.entity.enums.ContactStatus;
import com.ecomart.exception.ConflictException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.ContactMessageRepository;
import com.ecomart.service.ContactMessageService;
import com.ecomart.specification.ContactMessageSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactMessageServiceImpl implements ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;

    @Override
    @Transactional
    public ContactMessageResponse createMessage(CreateContactMessageRequest request) {
        ContactMessage message = ContactMessage.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .subject(request.getSubject().trim())
                .content(request.getContent().trim())
                .status(ContactStatus.NEW)
                .build();

        ContactMessage savedMessage = contactMessageRepository.save(message);
        return mapToResponse(savedMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContactMessageResponse> getAdminMessages(
            int page,
            int pageSize,
            ContactStatus status,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<ContactMessage> spec = ContactMessageSpecification.filterAdminMessages(status, keyword, fromDate, toDate);

        Page<ContactMessage> messagePage = contactMessageRepository.findAll(spec, pageable);
        List<ContactMessageResponse> items = messagePage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.from(items, messagePage);
    }

    @Override
    @Transactional(readOnly = true)
    public ContactMessageResponse getAdminMessageDetail(Long messageId) {
        ContactMessage message = contactMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Tin nhắn liên hệ không tồn tại với ID: " + messageId));
        return mapToResponse(message);
    }

    @Override
    @Transactional
    public ContactMessageResponse resolveMessage(Long messageId) {
        ContactMessage message = contactMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Tin nhắn liên hệ không tồn tại với ID: " + messageId));

        if (message.getStatus() == ContactStatus.RESOLVED) {
            throw new ConflictException("Tin nhắn liên hệ này đã được xử lý trước đó");
        }

        message.setStatus(ContactStatus.RESOLVED);
        message.setResolvedAt(LocalDateTime.now());

        ContactMessage updatedMessage = contactMessageRepository.save(message);
        return mapToResponse(updatedMessage);
    }

    private ContactMessageResponse mapToResponse(ContactMessage message) {
        return ContactMessageResponse.builder()
                .id(message.getId())
                .fullName(message.getFullName())
                .email(message.getEmail())
                .phone(message.getPhone())
                .subject(message.getSubject())
                .content(message.getContent())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .resolvedAt(message.getResolvedAt())
                .build();
    }
}
