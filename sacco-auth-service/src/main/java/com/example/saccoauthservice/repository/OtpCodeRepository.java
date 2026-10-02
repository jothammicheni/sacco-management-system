package com.example.saccoauthservice.repository;

import com.example.saccoauthservice.entity.OtpCode;
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
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    /** Find the most recent unused OTP for a user + purpose. */
    Optional<OtpCode> findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
            UUID userId, OtpCode.Purpose purpose);

    List<OtpCode> findByUserIdAndPurpose(UUID userId, OtpCode.Purpose purpose);

    /** Count how many OTPs have been sent recently (for rate limiting). */
    @Query("""
            SELECT COUNT(o) FROM OtpCode o
            WHERE o.userId = :userId
              AND o.purpose = :purpose
              AND o.createdAt > :since
            """)
    long countRecentForUser(
            @Param("userId") UUID userId,
            @Param("purpose") OtpCode.Purpose purpose,
            @Param("since") LocalDateTime since);

    // ---------- Invalidation ----------

    @Modifying
    @Query("""
            UPDATE OtpCode o SET o.usedAt = :now
            WHERE o.userId = :userId
              AND o.purpose = :purpose
              AND o.usedAt IS NULL
            """)
    int invalidateAllForUserAndPurpose(
            @Param("userId") UUID userId,
            @Param("purpose") OtpCode.Purpose purpose,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}