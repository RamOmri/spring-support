package com.nullify.supportportal.service;

import com.nullify.supportportal.domain.Role;
import com.nullify.supportportal.domain.User;
import com.nullify.supportportal.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User require(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("not authenticated");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("user not found"));
    }

    public boolean isStaff(User user) {
        return user.getRole() == Role.AGENT || user.getRole() == Role.ADMIN;
    }
}
