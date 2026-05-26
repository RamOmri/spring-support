package com.nullify.supportportal.repository;

import com.nullify.supportportal.domain.SavedSearch;
import com.nullify.supportportal.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {

    List<SavedSearch> findByUserOrderByCreatedAtDesc(User user);
}
