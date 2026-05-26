package com.nullify.supportportal.controller;

import com.nullify.supportportal.dto.SaveSearchRequest;
import com.nullify.supportportal.dto.SavedSearchResponse;
import com.nullify.supportportal.dto.TicketResponse;
import com.nullify.supportportal.service.SearchService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public String search(@RequestParam(name = "q", required = false, defaultValue = "") String q,
                         Authentication auth,
                         Model model) {
        List<TicketResponse> results = q.isBlank() ? List.of() : searchService.searchTickets(q, auth);
        List<SavedSearchResponse> saved = searchService.listSavedSearches(auth);
        model.addAttribute("currentUser", auth.getName());
        model.addAttribute("q", q);
        model.addAttribute("results", results);
        model.addAttribute("savedSearches", saved);
        return "search/index";
    }

    @PostMapping("/searches")
    public String saveSearch(@RequestParam("name") String name,
                             @RequestParam("query") String query,
                             Authentication auth) {
        searchService.saveSearch(new SaveSearchRequest(name, query), auth);
        return "redirect:/search?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
    }

    @PostMapping("/searches/{id}/delete")
    public String deleteSaved(@PathVariable long id, Authentication auth) {
        searchService.deleteSavedSearch(id, auth);
        return "redirect:/search";
    }
}
