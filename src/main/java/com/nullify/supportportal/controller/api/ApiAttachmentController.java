package com.nullify.supportportal.controller.api;

import com.nullify.supportportal.domain.Attachment;
import com.nullify.supportportal.dto.AttachmentResponse;
import com.nullify.supportportal.service.AttachmentService;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ApiAttachmentController {

    private final AttachmentService attachmentService;

    public ApiAttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(value = "/tickets/{ticketId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentResponse upload(@PathVariable long ticketId,
                                     @RequestParam("file") MultipartFile file,
                                     Authentication auth) throws IOException {
        return attachmentService.upload(ticketId, file, auth);
    }

    @GetMapping("/tickets/{ticketId}/attachments")
    public List<AttachmentResponse> list(@PathVariable long ticketId, Authentication auth) {
        return attachmentService.listForTicket(ticketId, auth);
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable long id, Authentication auth) {
        Attachment attachment = attachmentService.getForDownload(id, auth);
        Resource body = new FileSystemResource(attachment.getStoragePath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .contentLength(attachment.getSizeBytes())
                .body(body);
    }
}
