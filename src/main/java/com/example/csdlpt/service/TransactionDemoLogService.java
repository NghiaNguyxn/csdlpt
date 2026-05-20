package com.example.csdlpt.service;

import com.example.csdlpt.dto.response.TransactionEventLogResponse;
import com.example.csdlpt.entity.TransactionEventLog;
import com.example.csdlpt.entity.TransactionLog;
import com.example.csdlpt.enums.SiteCode;
import com.example.csdlpt.enums.TransactionStatus;
import com.example.csdlpt.repository.site_hn.HanoiTransactionEventLogRepository;
import com.example.csdlpt.repository.site_hn.HanoiTransactionLogRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionDemoLogService {

    HanoiTransactionLogRepository transactionLogRepository;
    HanoiTransactionEventLogRepository eventLogRepository;

    @Transactional(value = "hanoiTransactionManager")
    public void begin(String transactionId, List<SiteCode> participants) {
        transactionLogRepository.save(TransactionLog.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.INITIAL)
                .participants(toParticipants(participants))
                .build());
        event(transactionId, "TX_BEGIN", "COORDINATOR", "HN", null, null, "INITIAL", 0L,
                "Coordinator khởi tạo giao dịch phân tán; chuẩn bị pha growing của 2PL");
    }

    @Transactional(value = "hanoiTransactionManager")
    public void updateStatus(String transactionId, TransactionStatus status, List<SiteCode> participants, String message) {
        TransactionLog log = transactionLogRepository.findById(transactionId)
                .orElseGet(() -> TransactionLog.builder().transactionId(transactionId).build());
        log.setStatus(status);
        log.setParticipants(toParticipants(participants));
        transactionLogRepository.save(log);
        event(transactionId, "TX_STATUS", "COORDINATOR", "HN", null, null, status.name(), 0L, message);
    }

    @Transactional(value = "hanoiTransactionManager")
    public void lockRequest(String transactionId, SiteCode siteCode, String resourceKey, String lockMode) {
        event(transactionId, "LOCK_REQUEST", "LOCK_MANAGER", siteCode.name(), resourceKey, lockMode, "WAIT", 0L,
                "Transaction xin " + lockMode + " theo Centralized 2PL trước khi ghi inventory");
    }

    @Transactional(value = "hanoiTransactionManager")
    public void lockGranted(String transactionId, SiteCode siteCode, String resourceKey, String lockMode, long waitMillis) {
        event(transactionId, "LOCK_GRANTED", "LOCK_MANAGER", siteCode.name(), resourceKey, lockMode, "GRANTED", waitMillis,
                "Write lock được cấp; các transaction khác không được đọc/ghi tài nguyên này cho tới khi release");
    }

    @Transactional(value = "hanoiTransactionManager")
    public void lockTimeout(String transactionId, SiteCode siteCode, String resourceKey, String lockMode, long waitMillis, String reason) {
        event(transactionId, "LOCK_TIMEOUT", "LOCK_MANAGER", siteCode.name(), resourceKey, lockMode, "TIMEOUT", waitMillis,
                reason);
    }

    @Transactional(value = "hanoiTransactionManager")
    public void participantVote(String transactionId, SiteCode siteCode, String vote, String message) {
        event(transactionId, "PARTICIPANT_VOTE", "PARTICIPANT", siteCode.name(), null, null, vote, 0L, message);
    }

    @Transactional(value = "hanoiTransactionManager")
    public void releaseLock(String transactionId, SiteCode siteCode, String resourceKey) {
        event(transactionId, "LOCK_RELEASE", "LOCK_MANAGER", siteCode.name(), resourceKey, "WL", "RELEASED", 0L,
                "Giải phóng khóa sau khi đã hoàn tất thao tác ghi, đúng pha shrinking của 2PL");
    }

    public List<TransactionEventLogResponse> findRecentEvents() {
        return eventLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TransactionEventLogResponse> findEventsByTransactionId(String transactionId) {
        return eventLogRepository.findByTransactionIdOrderByCreatedAtAsc(transactionId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void event(String transactionId, String eventType, String actorRole, String siteCode,
            String resourceKey, String lockMode, String status, Long waitMillis, String message) {
        eventLogRepository.save(TransactionEventLog.builder()
                .transactionId(transactionId)
                .eventType(eventType)
                .actorRole(actorRole)
                .siteCode(siteCode)
                .resourceKey(resourceKey)
                .lockMode(lockMode)
                .status(status)
                .waitMillis(waitMillis)
                .message(message)
                .build());
    }

    private TransactionEventLogResponse toResponse(TransactionEventLog event) {
        return TransactionEventLogResponse.builder()
                .id(event.getId())
                .transactionId(event.getTransactionId())
                .eventType(event.getEventType())
                .actorRole(event.getActorRole())
                .siteCode(event.getSiteCode())
                .resourceKey(event.getResourceKey())
                .lockMode(event.getLockMode())
                .status(event.getStatus())
                .waitMillis(event.getWaitMillis())
                .message(event.getMessage())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private String toParticipants(List<SiteCode> participants) {
        if (participants == null || participants.isEmpty()) {
            return "[]";
        }
        return participants.stream().map(Enum::name).toList().toString();
    }
}
