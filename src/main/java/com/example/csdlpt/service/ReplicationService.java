package com.example.csdlpt.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.csdlpt.entity.ReplicationLog;
import com.example.csdlpt.enums.ReplicationAction;
import com.example.csdlpt.enums.ReplicationStatus;
import com.example.csdlpt.repository.site_dn.DanangProductRepository;
import com.example.csdlpt.repository.site_hcm.HcmProductRepository;
import com.example.csdlpt.repository.site_hn.HanoiReplicationLogRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReplicationService {

    DanangProductRepository danangProductRepository;
    HcmProductRepository hcmProductRepository;
    HanoiReplicationLogRepository logRepository;

    @Transactional(value = "hanoiTransactionManager")
    public void logChange(Long entityId, String entityType, ReplicationAction action) {
        // Replication log được lưu ở HN, mỗi target site có một log riêng để retry độc lập.
        ReplicationLog logDN = ReplicationLog.builder()
                .entityId(entityId)
                .entityType(entityType)
                .action(action.name())
                .targetSite("DN")
                .status(ReplicationStatus.PENDING)
                .build();

        ReplicationLog logHCM = ReplicationLog.builder()
                .entityId(entityId)
                .entityType(entityType)
                .action(action.name())
                .targetSite("HCM")
                .status(ReplicationStatus.PENDING)
                .build();

        logRepository.saveAll(List.of(logDN, logHCM));
    }

    @Transactional(value = "danangTransactionManager")
    public void replicateProductToDanang(Long id, String name, BigDecimal price, Integer categoryId, Boolean isActive) {
        danangProductRepository.replicateProduct(id.intValue(), name, price, categoryId, isActive);
    }

    @Transactional(value = "hcmTransactionManager")
    public void replicateProductToHcm(Long id, String name, BigDecimal price, Integer categoryId, Boolean isActive) {
        hcmProductRepository.replicateProduct(id.intValue(), name, price, categoryId, isActive);
    }

    @Transactional(value = "hanoiTransactionManager")
    public void markLogDone(ReplicationLog logEntry) {
        logEntry.setStatus(ReplicationStatus.DONE);
        logRepository.save(logEntry);
    }

    @Transactional(value = "hanoiTransactionManager")
    public void markLogRetry(ReplicationLog logEntry, String errorMessage) {
        // Chỉ tăng retry count ở bước retry; chuyển FAILED không tăng thêm lần nữa.
        logEntry.setStatus(ReplicationStatus.PENDING);
        logEntry.setErrorMessage(errorMessage);
        logEntry.setRetryCount(logEntry.getRetryCount() + 1);
        logRepository.save(logEntry);
    }

    @Transactional(value = "hanoiTransactionManager")
    public void markLogFailed(ReplicationLog logEntry, String errorMessage) {
        logEntry.setStatus(ReplicationStatus.FAILED);
        logEntry.setErrorMessage(errorMessage);
        logRepository.save(logEntry);
    }
}
