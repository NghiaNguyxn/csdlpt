package com.example.csdlpt.service;

import com.example.csdlpt.entity.ReplicationLog;
import com.example.csdlpt.enums.ReplicationAction;
import com.example.csdlpt.enums.ReplicationStatus;
import com.example.csdlpt.repository.site_dn.DanangProductRepository;
import com.example.csdlpt.repository.site_hcm.HcmProductRepository;
import com.example.csdlpt.repository.site_hn.HanoiReplicationLogRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReplicationService {

    DanangProductRepository danangProductRepository;
    HcmProductRepository hcmProductRepository;
    HanoiReplicationLogRepository logRepository;

    /**
     * Ghi log nhân bản cho các site replica (DN, HCM).
     * Thường gọi từ Master Site (HN).
     */
    @Transactional(value = "hanoiTransactionManager")
    public void logChange(Long entityId, String entityType, ReplicationAction action) {
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
    public void updateLogStatus(ReplicationLog logEntry, ReplicationStatus status, String errorMessage) {
        logEntry.setStatus(status);
        if (errorMessage != null) {
            logEntry.setErrorMessage(errorMessage);
            logEntry.setRetryCount(logEntry.getRetryCount() + 1);
        }
        logRepository.save(logEntry);
    }
}
