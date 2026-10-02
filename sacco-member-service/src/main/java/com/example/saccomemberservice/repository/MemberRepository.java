package com.example.saccomemberservice.repository;

import com.example.saccomemberservice.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {

    // ---------- Uniqueness checks (respect @SQLRestriction, so soft-deleted rows are ignored) ----------
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByNationalId(String nationalId);

    // ---------- Lookups ----------
    Optional<Member> findByEmail(String email);
    Optional<Member> findByPhoneNumber(String phoneNumber);
    Optional<Member> findByMemberNumber(String memberNumber);

    // ---------- Soft-delete aware operations ----------
    // @SQLRestriction("deleted_at IS NULL") hides soft-deleted rows from all of the above.
    // These two bypass it and let you inspect or restore deleted records.

    @Query(value = "SELECT * FROM members WHERE id = :id", nativeQuery = true)
    Optional<Member> findByIdIncludingDeleted(@Param("id") UUID id);

    @Modifying
    @Query(value = "UPDATE members SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    int restoreById(@Param("id") UUID id);

    @Modifying
    @Query(value = "UPDATE members SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id", nativeQuery = true)
    int softDeleteById(@Param("id") UUID id);
}