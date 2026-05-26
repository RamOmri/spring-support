package com.nullify.supportportal.service;

import com.nullify.supportportal.domain.Comment;
import com.nullify.supportportal.domain.Role;
import com.nullify.supportportal.domain.Ticket;
import com.nullify.supportportal.domain.TicketPriority;
import com.nullify.supportportal.domain.TicketStatus;
import com.nullify.supportportal.domain.User;
import com.nullify.supportportal.dto.CommentResponse;
import com.nullify.supportportal.dto.CreateCommentRequest;
import com.nullify.supportportal.dto.CreateTicketRequest;
import com.nullify.supportportal.dto.TicketResponse;
import com.nullify.supportportal.repository.CommentRepository;
import com.nullify.supportportal.repository.TicketRepository;
import com.nullify.supportportal.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final WebhookDispatchService webhookDispatchService;

    public TicketService(TicketRepository ticketRepository,
                         CommentRepository commentRepository,
                         UserRepository userRepository,
                         CurrentUserService currentUserService,
                         WebhookDispatchService webhookDispatchService) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.webhookDispatchService = webhookDispatchService;
    }

    @Transactional(readOnly = true)
    public List<Ticket> listVisible(Authentication authentication) {
        User user = currentUserService.require(authentication);
        if (currentUserService.isStaff(user)) {
            return ticketRepository.findAllByOrderByCreatedAtDesc();
        }
        return ticketRepository.findByCustomerOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listVisibleAsResponses(Authentication authentication) {
        return listVisible(authentication).stream().map(TicketResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Ticket getVisible(long ticketId, Authentication authentication) {
        User user = currentUserService.require(authentication);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("ticket not found: " + ticketId));
        if (!currentUserService.isStaff(user) && !ticket.getCustomer().getId().equals(user.getId())) {
            throw new AccessDeniedException("not your ticket");
        }
        return ticket;
    }

    @Transactional(readOnly = true)
    public TicketResponse getVisibleAsResponse(long ticketId, Authentication authentication) {
        return TicketResponse.from(getVisible(ticketId, authentication));
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request, Authentication authentication) {
        User user = currentUserService.require(authentication);
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description() == null ? "" : request.description());
        ticket.setPriority(request.priority() == null ? TicketPriority.NORMAL : request.priority());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCustomer(user);
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse updateStatus(long ticketId, TicketStatus newStatus, Authentication authentication) {
        User user = currentUserService.require(authentication);
        if (!currentUserService.isStaff(user)) {
            throw new AccessDeniedException("only agents/admins can change ticket status");
        }
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("ticket not found: " + ticketId));
        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(newStatus);
        Ticket saved = ticketRepository.save(ticket);
        if (oldStatus != newStatus) {
            String payload = "{\"event\":\"ticket.status_changed\",\"ticket_id\":" + saved.getId()
                    + ",\"old_status\":\"" + oldStatus + "\",\"new_status\":\"" + newStatus + "\"}";
            webhookDispatchService.dispatch("ticket.status_changed", payload);
        }
        return TicketResponse.from(saved);
    }

    @Transactional
    public TicketResponse assignAgent(long ticketId, long agentId, Authentication authentication) {
        User actor = currentUserService.require(authentication);
        if (actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("only admins can assign agents");
        }
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new NoSuchElementException("agent not found: " + agentId));
        if (agent.getRole() != Role.AGENT && agent.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("user is not an agent");
        }
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("ticket not found: " + ticketId));
        ticket.setAssignedAgent(agent);
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public CommentResponse addComment(long ticketId, CreateCommentRequest request, Authentication authentication) {
        Ticket ticket = getVisible(ticketId, authentication);
        User author = currentUserService.require(authentication);
        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setBody(request.body());
        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(long ticketId, Authentication authentication) {
        Ticket ticket = getVisible(ticketId, authentication);
        return commentRepository.findByTicketOrderByCreatedAtAsc(ticket).stream()
                .map(CommentResponse::from)
                .toList();
    }
}
