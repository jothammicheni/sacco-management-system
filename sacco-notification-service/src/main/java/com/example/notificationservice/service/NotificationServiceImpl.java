package com.example.notificationservice.service;

import com.example.notificationservice.dto.NotificationLogResponseDto;
import com.example.notificationservice.dto.NotificationRequestDto;
import com.example.notificationservice.dto.SendEmailResultDto;
import com.example.notificationservice.entity.NotificationLog;
import com.example.notificationservice.repository.NotificationLogRepository;
import com.example.notificationservice.service.EmailSender;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationLogRepository logRepo;
    private final EmailSender emailSender;

    // ==========================================================
    // 1. SEND
    // ==========================================================
    @Override
    @Transactional
    public NotificationLogResponseDto send(NotificationRequestDto request,
                                           String deduplicationKey) {

        // ---------- 1. Idempotency ----------
        if (deduplicationKey != null && !deduplicationKey.isBlank()) {
            Optional<NotificationLog> existing = logRepo.findByKafkaEventId(deduplicationKey);
            if (existing.isPresent()) {
                log.info("Notification already sent for key={}, skipping", deduplicationKey);
                return NotificationLogResponseDto.fromEntity(existing.get());
            }
        }

        // ---------- 2. Dispatch — EMAIL only ----------
        if (request.getChannel() != NotificationRequestDto.Channel.EMAIL) {
            throw new UnsupportedOperationException(
                    "Channel " + request.getChannel() + " is not yet implemented. Only EMAIL is supported.");
        }

        SendEmailResultDto result = emailSender.send(
                request.getRecipientAddress(),
                "SACCO Notification",
                request.getMessageBody());

        boolean success = result.isSuccess();
        String providerReference = result.getProviderReference();
        String errorMessage = result.getErrorMessage();

        // ---------- 3. Persist log ----------
        String eventId = (deduplicationKey != null && !deduplicationKey.isBlank())
                ? deduplicationKey
                : "manual-" + UUID.randomUUID();

        NotificationLog notificationLog = NotificationLog.builder()
                .memberId(request.getMemberId())
                .channel(NotificationLog.Channel.valueOf(request.getChannel().name()))
                .recipientAddress(request.getRecipientAddress())
                .messageBody(request.getMessageBody())
                .status(success ? NotificationLog.Status.SENT : NotificationLog.Status.FAILED)
                .kafkaEventId(eventId)
                .build();

        NotificationLog saved = logRepo.save(notificationLog);

        if (success) {
            log.info("Notification SENT to {} via EMAIL (ref={})",
                    request.getRecipientAddress(), providerReference);
        } else {
            log.error("Notification FAILED to {} via EMAIL: {}",
                    request.getRecipientAddress(), errorMessage);
        }

        return NotificationLogResponseDto.fromEntity(saved);
    }
    // ==========================================================
    // 2. READ
    // ==========================================================
    @Override
    @Transactional(readOnly = true)
    public NotificationLogResponseDto getById(Long id) {
        NotificationLog log = logRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Notification not found: " + id));
        return NotificationLogResponseDto.fromEntity(log);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationLogResponseDto getByKafkaEventId(String kafkaEventId) {
        NotificationLog log = logRepo.findByKafkaEventId(kafkaEventId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Notification not found for key: " + kafkaEventId));
        return NotificationLogResponseDto.fromEntity(log);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationLogResponseDto> getByMemberId(UUID memberId, Pageable pageable) {
        return logRepo.findByMemberId(memberId, pageable)
                .map(NotificationLogResponseDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationLogResponseDto> getByStatus(String status, Pageable pageable) {
        NotificationLog.Status s = NotificationLog.Status.valueOf(status.toUpperCase());
        return logRepo.findByStatus(s, pageable)
                .map(NotificationLogResponseDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationLogResponseDto> getByChannel(String channel, Pageable pageable) {
        NotificationLog.Channel c = NotificationLog.Channel.valueOf(channel.toUpperCase());
        return logRepo.findByChannel(c, pageable)
                .map(NotificationLogResponseDto::fromEntity);
    }

    // ==========================================================
    // 3. MAINTENANCE
    // ==========================================================
    @Override
    @Transactional
    public void softDelete(Long id) {
        NotificationLog log = logRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Notification not found: " + id));
        logRepo.delete(log);   // triggers @SQLDelete → UPDATE ... SET deleted_at
    }
}