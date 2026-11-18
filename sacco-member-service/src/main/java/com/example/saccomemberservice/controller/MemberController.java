package com.example.saccomemberservice.controller;

import com.example.saccomemberservice.dto.MemberRequestDto;
import com.example.saccomemberservice.dto.MemberResponseDto;
import com.example.saccomemberservice.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;



@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;

    // ==========================================================
    // SELF-SERVICE (any authenticated user)
    // ==========================================================
    @GetMapping("/me")
    public MemberResponseDto myProfile(@RequestHeader("X-User-Id") UUID userId) {
        return memberService.getById(userId);
    }

    @PutMapping("/me")
    public MemberResponseDto updateSelf(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody MemberRequestDto request) {
        return memberService.update(userId, request);
    }

    // ==========================================================
    // ADMIN MANAGEMENT
    // ==========================================================
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponseDto create(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody MemberRequestDto request) {
        requireAdmin(role);
        return memberService.create(request);
    }

    @GetMapping
    public Page<MemberResponseDto> getAll(
            @RequestHeader("X-User-Role") String role,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        requireAdmin(role);
        return memberService.getAll(pageable);
    }

    @GetMapping("/{id:[0-9a-fA-F-]{36}}")
    public MemberResponseDto getById(
            @RequestHeader("X-User-Id") UUID callerId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID id) {
        MemberResponseDto member = memberService.getById(id);
        if (!"ADMIN".equals(role) && !member.getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only view your own profile");
        }
        return member;
    }

    @GetMapping("/number/{memberNumber}")
    public MemberResponseDto getByMemberNumber(
            @RequestHeader("X-User-Role") String role,
            @PathVariable String memberNumber) {
        requireAdmin(role);
        return memberService.getByMemberNumber(memberNumber);
    }

    @PutMapping("/{id:[0-9a-fA-F-]{36}}")
    public MemberResponseDto update(
            @RequestHeader("X-User-Id") UUID callerId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID id,
            @Valid @RequestBody MemberRequestDto request) {
        if (!"ADMIN".equals(role) && !id.equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only update your own profile");
        }
        return memberService.update(id, request);
    }

    @DeleteMapping("/{id:[0-9a-fA-F-]{36}}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID id) {
        requireAdmin(role);
        memberService.softDelete(id);
    }

    @PostMapping("/{id:[0-9a-fA-F-]{36}}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID id) {
        requireAdmin(role);
        memberService.restore(id);
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        }
    }
}