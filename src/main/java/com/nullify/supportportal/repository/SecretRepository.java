package com.nullify.supportportal.repository;

import com.nullify.supportportal.domain.Secret;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SecretRepository extends JpaRepository<Secret, Long> {

    Optional<Secret> findByKey(String key);
}
