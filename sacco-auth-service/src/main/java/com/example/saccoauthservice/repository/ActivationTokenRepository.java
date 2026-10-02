package com.example.saccoauthservice.repository;

import com.example.saccoauthservice.entity.ActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, String> {

    Optional<ActivationToken> findByToken(String token);

    List<ActivationToken> findByUserId(UUID userId);

    List<ActivationToken> findByUserIdAndUsedAtIsNull(UUID userId);

    // ---------- Cleanup ----------

    @Modifying
    @Query("DELETE FROM ActivationToken a WHERE a.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("UPDATE ActivationToken a SET a.usedAt = :now WHERE a.userId = :userId AND a.usedAt IS NULL")
    int invalidateAllForUser(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}