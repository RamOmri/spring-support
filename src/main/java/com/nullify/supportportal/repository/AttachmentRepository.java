package com.nullify.supportportal.repository;

import com.nullify.supportportal.domain.Attachment;
import com.nullify.supportportal.domain.Ticket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByTicketOrderByCreatedAtAsc(Ticket ticket);
}
