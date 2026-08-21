package com.ecomart.service;

import com.ecomart.dto.request.CreateContactMessageRequest;
import com.ecomart.dto.response.ContactMessageResponse;
import com.ecomart.dto.response.PageResponse;
import com.ecomart.entity.ContactMessage;
import com.ecomart.entity.enums.ContactStatus;
import com.ecomart.exception.ConflictException;
import com.ecomart.exception.ResourceNotFoundException;
import com.ecomart.repository.ContactMessageRepository;
import com.ecomart.service.impl.ContactMessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactMessageServiceTest {

    @Mock
    private ContactMessageRepository contactMessageRepository;

    @InjectMocks
    private ContactMessageServiceImpl contactMessageService;

    private ContactMessage message;

    @BeforeEach
    void setUp() {
        message = ContactMessage.builder()
                .id(1L)
                .fullName("Nguyen Van A")
                .email("user@example.com")
                .phone("0901234567")
                .subject("Hỏi về sản phẩm bình giữ nhiệt")
                .content("Sản phẩm này có giữ nhiệt được 24 tiếng không?")
                .status(ContactStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Khách gửi tin nhắn liên hệ thành công (status = NEW)")
    void createMessage_Success() {
        CreateContactMessageRequest request = CreateContactMessageRequest.builder()
                .fullName("Nguyen Van A")
                .email("user@example.com")
                .phone("0901234567")
                .subject("Hỏi về sản phẩm bình giữ nhiệt")
                .content("Sản phẩm này có giữ nhiệt được 24 tiếng không?")
                .build();

        when(contactMessageRepository.save(any(ContactMessage.class))).thenReturn(message);

        ContactMessageResponse response = contactMessageService.createMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(ContactStatus.NEW);
        verify(contactMessageRepository).save(any(ContactMessage.class));
    }

    @Test
    @DisplayName("Admin lấy danh sách tin nhắn phân trang thành công")
    void getAdminMessages_Success() {
        when(contactMessageRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(message)));

        PageResponse<ContactMessageResponse> response = contactMessageService.getAdminMessages(1, 10, null, null, null, null);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Admin xem chi tiết tin nhắn thành công")
    void getAdminMessageDetail_Success() {
        when(contactMessageRepository.findById(1L)).thenReturn(Optional.of(message));

        ContactMessageResponse response = contactMessageService.getAdminMessageDetail(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Ném 404 khi tin nhắn không tồn tại")
    void getAdminMessageDetail_Throws404_WhenNotFound() {
        when(contactMessageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactMessageService.getAdminMessageDetail(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tin nhắn liên hệ không tồn tại");
    }

    @Test
    @DisplayName("Admin đánh dấu xử lý tin nhắn thành công (status = RESOLVED)")
    void resolveMessage_Success() {
        when(contactMessageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(contactMessageRepository.save(any(ContactMessage.class))).thenReturn(message);

        ContactMessageResponse response = contactMessageService.resolveMessage(1L);

        assertThat(response).isNotNull();
        assertThat(message.getStatus()).isEqualTo(ContactStatus.RESOLVED);
        assertThat(message.getResolvedAt()).isNotNull();
        verify(contactMessageRepository).save(message);
    }

    @Test
    @DisplayName("Ném 409 Conflict khi đánh dấu tin nhắn đã RESOLVED trước đó")
    void resolveMessage_Throws409_WhenAlreadyResolved() {
        message.setStatus(ContactStatus.RESOLVED);
        when(contactMessageRepository.findById(1L)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> contactMessageService.resolveMessage(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("đã được xử lý trước đó");
    }
}
