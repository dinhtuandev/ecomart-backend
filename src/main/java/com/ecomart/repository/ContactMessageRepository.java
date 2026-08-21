package com.ecomart.repository;

import com.ecomart.entity.ContactMessage;
import com.ecomart.entity.enums.ContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long>, JpaSpecificationExecutor<ContactMessage> {

    long countByStatus(ContactStatus status);
}
