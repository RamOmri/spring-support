package com.nullify.supportportal.controller.api;

import com.nullify.supportportal.dto.SaveSearchRequest;
import com.nullify.supportportal.dto.SavedSearchResponse;
import com.nullify.supportportal.dto.TicketResponse;
import com.nullify.supportportal.service.SearchService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ApiSearchController {

    private final SearchService searchService;

    public ApiSearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/tickets/search")
    public List<TicketResponse> searchTickets(@RequestParam(name = "q", required = false) String q,
                                              Authentication auth) {
        return searchService.searchTickets(q, auth);
    }

    @PostMapping("/searches")
    public SavedSearchResponse saveSearch(@Valid @RequestBody SaveSearchRequest request,
                                          Authentication auth) {
        return searchService.saveSearch(request, auth);
    }

    @GetMapping("/searches")
    public List<SavedSearchResponse> listSavedSearches(Authentication auth) {
        return searchService.listSavedSearches(auth);
    }

    @DeleteMapping("/searches/{id}")
    public ResponseEntity<Void> deleteSavedSearch(@PathVariable long id, Authentication auth) {
        searchService.deleteSavedSearch(id, auth);
        return ResponseEntity.noContent().build();
    }
}
