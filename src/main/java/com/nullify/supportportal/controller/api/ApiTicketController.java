package com.nullify.supportportal.controller.api;

import com.nullify.supportportal.dto.AssignAgentRequest;
import com.nullify.supportportal.dto.CommentResponse;
import com.nullify.supportportal.dto.CreateCommentRequest;
import com.nullify.supportportal.dto.CreateTicketRequest;
import com.nullify.supportportal.dto.TicketResponse;
import com.nullify.supportportal.dto.UpdateTicketStatusRequest;
import com.nullify.supportportal.service.TicketService;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class ApiTicketController {

    private final TicketService ticketService;

    public ApiTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public List<TicketResponse> list(Authentication auth) {
        return ticketService.listVisibleAsResponses(auth);
    }

    @PostMapping
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request, Authentication auth) {
        return ticketService.create(request, auth);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable long id, Authentication auth) {
        return ticketService.getVisibleAsResponse(id, auth);
    }

    @PostMapping("/{id}/status")
    public TicketResponse updateStatus(@PathVariable long id,
                                       @Valid @RequestBody UpdateTicketStatusRequest request,
                                       Authentication auth) {
        return ticketService.updateStatus(id, request.status(), auth);
    }

    @PostMapping("/{id}/assign")
    public TicketResponse assignAgent(@PathVariable long id,
                                      @Valid @RequestBody AssignAgentRequest request,
                                      Authentication auth) {
        return ticketService.assignAgent(id, request.agentId(), auth);
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> listComments(@PathVariable long id, Authentication auth) {
        return ticketService.listComments(id, auth);
    }

    @PostMapping("/{id}/comments")
    public CommentResponse addComment(@PathVariable long id,
                                       @Valid @RequestBody CreateCommentRequest request,
                                       Authentication auth) {
        return ticketService.addComment(id, request, auth);
    }
}
