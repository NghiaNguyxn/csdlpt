package com.example.csdlpt.service;

import com.example.csdlpt.dto.response.TransactionLogResponse;
import com.example.csdlpt.entity.TransactionLog;
import com.example.csdlpt.enums.SiteCode;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.common.DistributedTransactionLogRepository;
import com.example.csdlpt.repository.site_dn.DanangTransactionLogRepository;
import com.example.csdlpt.repository.site_hcm.HcmTransactionLogRepository;
import com.example.csdlpt.repository.site_hn.HanoiTransactionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionLogService {
    private final HanoiTransactionLogRepository hanoiTransactionLogRepository;
    private final DanangTransactionLogRepository danangTransactionLogRepository;
    private final HcmTransactionLogRepository hcmTransactionLogRepository;

    public List<TransactionLogResponse> getTransactions() {
        List<TransactionLogResponse> responses = new ArrayList<>();
        for (SiteCode site : SiteCode.values()) {
            txRepo(site).findAll().forEach(log -> responses.add(toResponse(log, site.name())));
        }
        responses.sort(Comparator.comparing(TransactionLogResponse::getUpdatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return responses;
    }

    public TransactionLogResponse getTransaction(String id) {
        for (SiteCode site : SiteCode.values()) {
            Optional<TransactionLog> log = txRepo(site).findById(id);
            if (log.isPresent()) {
                return toResponse(log.get(), site.name());
            }
        }
        throw new AppException(ErrorCode.INVALID_KEY, "Không tìm thấy transaction log: " + id);
    }

    private TransactionLogResponse toResponse(TransactionLog log, String sourceSite) {
        return TransactionLogResponse.builder()
                .transactionId(log.getTransactionId())
                .status(log.getStatus())
                .participants(log.getParticipants())
                .createdAt(log.getCreatedAt())
                .updatedAt(log.getUpdatedAt())
                .sourceSite(sourceSite)
                .build();
    }

    private DistributedTransactionLogRepository txRepo(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangTransactionLogRepository;
            case HCM -> hcmTransactionLogRepository;
            default -> hanoiTransactionLogRepository;
        };
    }
}
