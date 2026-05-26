package com.nullify.supportportal.service;

import com.nullify.supportportal.domain.Role;
import com.nullify.supportportal.domain.User;
import com.nullify.supportportal.dto.UserResponse;
import com.nullify.supportportal.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse setRole(long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("user not found: " + userId));
        user.setRole(role);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse setEnabled(long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("user not found: " + userId));
        user.setEnabled(enabled);
        return UserResponse.from(userRepository.save(user));
    }
}
