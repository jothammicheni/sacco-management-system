package com.example.saccomemberservice.dto;

import com.example.saccomemberservice.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponseDto {

    private UUID id;
    private String memberNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String nationalId;
    private Member.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MemberResponseDto fromEntity(Member m) {
        return MemberResponseDto.builder()
                .id(m.getId())
                .memberNumber(m.getMemberNumber())
                .firstName(m.getFirstName())
                .lastName(m.getLastName())
                .email(m.getEmail())
                .phoneNumber(m.getPhoneNumber())
                .nationalId(m.getNationalId())
                .status(m.getStatus())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}