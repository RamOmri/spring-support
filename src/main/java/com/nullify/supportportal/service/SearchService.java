package com.nullify.supportportal.service;

import com.nullify.supportportal.domain.SavedSearch;
import com.nullify.supportportal.domain.User;
import com.nullify.supportportal.dto.SaveSearchRequest;
import com.nullify.supportportal.dto.SavedSearchResponse;
import com.nullify.supportportal.dto.TicketResponse;
import com.nullify.supportportal.repository.SavedSearchRepository;
import com.nullify.supportportal.repository.TicketRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class SearchService {

    private final TicketRepository ticketRepository;
    private final SavedSearchRepository savedSearchRepository;
    private final CurrentUserService currentUserService;

    public SearchService(TicketRepository ticketRepository,
                         SavedSearchRepository savedSearchRepository,
                         CurrentUserService currentUserService) {
        this.ticketRepository = ticketRepository;
        this.savedSearchRepository = savedSearchRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> searchTickets(String query, Authentication authentication) {
        User user = currentUserService.require(authentication);
        String pattern = "%" + (query == null ? "" : query) + "%";
        if (currentUserService.isStaff(user)) {
            return ticketRepository.searchByPattern(pattern).stream()
                    .map(TicketResponse::from)
                    .toList();
        }
        return ticketRepository.searchByPatternForCustomer(pattern, user.getId()).stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Transactional
    public SavedSearchResponse saveSearch(SaveSearchRequest request, Authentication authentication) {
        User user = currentUserService.require(authentication);
        SavedSearch saved = new SavedSearch();
        saved.setUser(user);
        saved.setName(request.name());
        saved.setQuery(request.query());
        return SavedSearchResponse.from(savedSearchRepository.save(saved));
    }

    @Transactional(readOnly = true)
    public List<SavedSearchResponse> listSavedSearches(Authentication authentication) {
        User user = currentUserService.require(authentication);
        return savedSearchRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(SavedSearchResponse::from)
                .toList();
    }

    @Transactional
    public void deleteSavedSearch(long id, Authentication authentication) {
        User user = currentUserService.require(authentication);
        SavedSearch saved = savedSearchRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("saved search not found: " + id));
        if (!saved.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("not your saved search");
        }
        savedSearchRepository.delete(saved);
    }
}
