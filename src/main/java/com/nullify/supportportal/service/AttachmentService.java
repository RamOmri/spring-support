package com.nullify.supportportal.service;

import com.nullify.supportportal.domain.Attachment;
import com.nullify.supportportal.domain.Ticket;
import com.nullify.supportportal.domain.User;
import com.nullify.supportportal.dto.AttachmentResponse;
import com.nullify.supportportal.repository.AttachmentRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TicketService ticketService;
    private final CurrentUserService currentUserService;
    private final Path uploadsRoot;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             TicketService ticketService,
                             CurrentUserService currentUserService,
                             @Value("${app.uploads.dir:/app/uploads}") String uploadsDir) {
        this.attachmentRepository = attachmentRepository;
        this.ticketService = ticketService;
        this.currentUserService = currentUserService;
        this.uploadsRoot = Paths.get(uploadsDir).toAbsolutePath().normalize();
    }

    @Transactional
    public AttachmentResponse upload(long ticketId, MultipartFile file, Authentication authentication) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }
        Ticket ticket = ticketService.getVisible(ticketId, authentication);
        User uploader = currentUserService.require(authentication);

        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String safeName = original.replaceAll("[/\\\\]", "_");
        String storedName = UUID.randomUUID() + "-" + safeName;
        Path ticketDir = uploadsRoot.resolve(String.valueOf(ticketId));
        Files.createDirectories(ticketDir);
        Path dest = ticketDir.resolve(storedName);
        try (var in = file.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }

        Attachment attachment = new Attachment();
        attachment.setTicket(ticket);
        attachment.setFilename(safeName);
        attachment.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        attachment.setSizeBytes(file.getSize());
        attachment.setStoragePath(dest.toString());
        attachment.setUploadedBy(uploader);
        return AttachmentResponse.from(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> listForTicket(long ticketId, Authentication authentication) {
        Ticket ticket = ticketService.getVisible(ticketId, authentication);
        return attachmentRepository.findByTicketOrderByCreatedAtAsc(ticket).stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Attachment getForDownload(long attachmentId, Authentication authentication) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NoSuchElementException("attachment not found: " + attachmentId));
        ticketService.getVisible(attachment.getTicket().getId(), authentication);
        return attachment;
    }
}
