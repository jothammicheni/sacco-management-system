package com.example.saccomemberservice.controller;

import com.example.saccomemberservice.dto.MemberRequestDto;
import com.example.saccomemberservice.dto.MemberResponseDto;
import com.example.saccomemberservice.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // ----------------------------------------------------------
    // CREATE
    // POST /api/v1/members
    // ----------------------------------------------------------
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponseDto create(@Valid @RequestBody MemberRequestDto request) {
        return memberService.create(request);
    }

    // ----------------------------------------------------------
    // READ (by id)
    // GET /api/v1/members/{id}
    // ----------------------------------------------------------
    @GetMapping("/{id}")
    public MemberResponseDto getById(@PathVariable UUID id) {
        return memberService.getById(id);
    }

    // ----------------------------------------------------------
    // READ (by member number)
    // GET /api/v1/members/number/{memberNumber}
    // ----------------------------------------------------------
    @GetMapping("/number/{memberNumber}")
    public MemberResponseDto getByMemberNumber(@PathVariable String memberNumber) {
        return memberService.getByMemberNumber(memberNumber);
    }

    // ----------------------------------------------------------
    // READ (paginated list)
    // GET /api/v1/members?page=0&size=20&sort=createdAt,desc
    // ----------------------------------------------------------
    @GetMapping
    public Page<MemberResponseDto> getAll(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return memberService.getAll(pageable);
    }

    // ----------------------------------------------------------
    // UPDATE
    // PUT /api/v1/members/{id}
    // ----------------------------------------------------------
    @PutMapping("/{id}")
    public MemberResponseDto update(@PathVariable UUID id,
                                    @Valid @RequestBody MemberRequestDto request) {
        return memberService.update(id, request);
    }

    // ----------------------------------------------------------
    // DELETE (soft)
    // DELETE /api/v1/members/{id}
    // ----------------------------------------------------------
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(@PathVariable UUID id) {
        memberService.softDelete(id);
    }

    // ----------------------------------------------------------
    // RESTORE
    // POST /api/v1/members/{id}/restore
    // ----------------------------------------------------------
    @PostMapping("/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(@PathVariable UUID id) {
        memberService.restore(id);
    }
}