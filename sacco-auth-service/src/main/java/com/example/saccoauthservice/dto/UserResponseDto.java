package com.example.saccoauthservice.dto;

import com.example.saccoauthservice.entity.User;
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
public class UserResponseDto {

    private UUID id;
    private String phoneNumber;
    private String email;
    private User.Role role;
    private User.Status status;
    private boolean hasPassword;
    private boolean hasPin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponseDto fromEntity(User u) {
        return UserResponseDto.builder()
                .id(u.getId())
//                .phoneNumber(u.getPhoneNumber())
                .email(u.getEmail())
                .role(u.getRole())
                .status(u.getStatus())
                .hasPassword(u.hasPassword())
                .hasPin(u.hasPin())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}