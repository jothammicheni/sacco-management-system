package com.example.saccomemberservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_members_member_number", columnNames = "member_number"),
                @UniqueConstraint(name = "uq_members_email",         columnNames = "email"),
                @UniqueConstraint(name = "uq_members_phone_number",  columnNames = "phone_number"),
                @UniqueConstraint(name = "uq_members_national_id",   columnNames = "national_id")
        },
        indexes = {
                @Index(name = "idx_members_status",     columnList = "status"),
                @Index(name = "idx_members_deleted_at", columnList = "deleted_at")
        }
)
@SQLDelete(sql = "UPDATE members SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(name = "member_number", nullable = false, length = 50)
    private String memberNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "national_id", nullable = false, length = 50)
    private String nationalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ---------- Enum ----------
    public enum Status {
        PENDING,
        ACTIVE,
        SUSPENDED
    }

    // ---------- Lifecycle hook ----------
    @PrePersist
    void onCreate() {
        if (status == null) {
            status = Status.PENDING;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}