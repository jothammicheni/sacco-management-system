package com.example.saccoauthservice.repository;

import com.example.saccoauthservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ---------- Lookups (login identifier is phone) ----------

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByEmail(String email);

    // ---------- Uniqueness checks ----------

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    // ---------- Filters ----------

    List<User> findByRole(User.Role role);

    Page<User> findByStatus(User.Status status, Pageable pageable);

    Page<User> findByRoleAndStatus(User.Role role, User.Status status, Pageable pageable);

    // ---------- Counts (for monitoring) ----------

    long countByStatus(User.Status status);
}