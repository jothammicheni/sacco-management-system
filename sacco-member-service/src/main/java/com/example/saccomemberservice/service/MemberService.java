package com.example.saccomemberservice.service;

import com.example.saccomemberservice.dto.MemberRequestDto;
import com.example.saccomemberservice.dto.MemberResponseDto;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface MemberService {

    MemberResponseDto create(MemberRequestDto request);

    MemberResponseDto getById(UUID id);

    MemberResponseDto getByMemberNumber(String memberNumber);


    Page<MemberResponseDto> getAll(Pageable pageable);


    MemberResponseDto update(UUID id, MemberRequestDto request);

    void softDelete(UUID id);

    void restore(UUID id);
}