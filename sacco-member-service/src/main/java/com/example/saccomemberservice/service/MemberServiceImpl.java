package com.example.saccomemberservice.service;

import com.example.saccomemberservice.dto.MemberRequestDto;
import com.example.saccomemberservice.dto.MemberResponseDto;
import com.example.saccomemberservice.entity.Member;
import com.example.saccomemberservice.kafka.events.OutboxEventWriter;
import com.example.saccomemberservice.repository.MemberRepository;
import com.example.saccomemberservice.service.MemberService;
import com.example.saccomemberservice.utils.IdGenerator;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepo;
    private final OutboxEventWriter outboxWriter;

    @Override
    @Transactional
    public MemberResponseDto create(MemberRequestDto request) {
        if (memberRepo.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already exists: " + request.getEmail());
        }
        if (memberRepo.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalStateException("Phone number already exists: " + request.getPhoneNumber());
        }
        if (memberRepo.existsByNationalId(request.getNationalId())) {
            throw new IllegalStateException("National ID already exists: " + request.getNationalId());
        }

        Member member = Member.builder()
                .memberNumber(IdGenerator.generateMemberNo())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .nationalId(request.getNationalId())
                .build();

        Member savedMember = memberRepo.save(member);

        // Write the outbox row in the same transaction — atomic with the member row.
        outboxWriter.writeMemberCreated(savedMember);

        return MemberResponseDto.fromEntity(savedMember);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponseDto getById(UUID id) {
        Member member = memberRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found: " + id));
        return MemberResponseDto.fromEntity(member);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponseDto getByMemberNumber(String memberNumber) {
        Member member = memberRepo.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new RuntimeException("Member number not found: " + memberNumber));
        return MemberResponseDto.fromEntity(member);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MemberResponseDto> getAll(Pageable pageable) {
        return memberRepo.findAll(pageable).map(MemberResponseDto::fromEntity);
    }

    @Override
    @Transactional
    public MemberResponseDto update(UUID id, MemberRequestDto request) {
        Member member = memberRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found: " + id));

        member.setFirstName(request.getFirstName());
        member.setLastName(request.getLastName());
        member.setEmail(request.getEmail());
        member.setPhoneNumber(request.getPhoneNumber());
        member.setNationalId(request.getNationalId());

        Member savedMember = memberRepo.save(member);
        outboxWriter.writeMemberUpdated(savedMember);

        return MemberResponseDto.fromEntity(savedMember);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        Member member = memberRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found: " + id));
        memberRepo.delete(member);
        outboxWriter.writeMemberDeleted(member);
    }

    @Override
    @Transactional
    public void restore(UUID id) {
        int rows = memberRepo.restoreById(id);
        if (rows == 0) {
            throw new RuntimeException("Member not found: " + id);
        }
    }
}