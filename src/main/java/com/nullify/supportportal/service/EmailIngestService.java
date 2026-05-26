package com.nullify.supportportal.service;

import com.nullify.supportportal.domain.Ticket;
import com.nullify.supportportal.domain.TicketPriority;
import com.nullify.supportportal.domain.TicketStatus;
import com.nullify.supportportal.domain.User;
import com.nullify.supportportal.dto.TicketResponse;
import com.nullify.supportportal.repository.TicketRepository;
import com.nullify.supportportal.repository.UserRepository;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Service
public class EmailIngestService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public EmailIngestService(UserRepository userRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public TicketResponse ingest(String rawMessage) throws MessagingException, IOException {
        if (rawMessage == null || rawMessage.isBlank()) {
            throw new IllegalArgumentException("empty email payload");
        }
        Session session = Session.getInstance(new Properties());
        MimeMessage mime;
        try (var in = new ByteArrayInputStream(rawMessage.getBytes(StandardCharsets.UTF_8))) {
            mime = new MimeMessage(session, in);
        }

        String fromEmail = extractFrom(mime);
        if (fromEmail == null) {
            throw new IllegalArgumentException("email has no From header");
        }
        String subject = mime.getSubject() == null ? "(no subject)" : mime.getSubject();
        String body = extractPlainText(mime);

        User customer = userRepository.findByEmail(fromEmail)
                .orElseThrow(() -> new IllegalArgumentException("no user with email " + fromEmail));

        Ticket ticket = new Ticket();
        ticket.setTitle(subject);
        ticket.setDescription(body == null ? "" : body);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(TicketPriority.NORMAL);
        ticket.setCustomer(customer);
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    private String extractFrom(MimeMessage mime) throws MessagingException {
        Address[] from = mime.getFrom();
        if (from == null || from.length == 0) return null;
        Address a = from[0];
        return (a instanceof InternetAddress ia) ? ia.getAddress() : a.toString();
    }

    private String extractPlainText(Part part) throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            return (String) part.getContent();
        }
        if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String text = extractPlainText(mp.getBodyPart(i));
                if (text != null) return text;
            }
        }
        if (part.isMimeType("text/*")) {
            Object content = part.getContent();
            return content == null ? null : content.toString();
        }
        return null;
    }
}
