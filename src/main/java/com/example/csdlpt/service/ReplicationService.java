package com.example.csdlpt.service;

import com.example.csdlpt.entity.ReplicationLog;
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

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReplicationService {

    DanangProductRepository danangProductRepository;
    HcmProductRepository hcmProductRepository;
    HanoiReplicationLogRepository logRepository;

    @Transactional(value = "danangTransactionManager")
    public void replicateToDanang(Integer id, String name, BigDecimal price, Integer categoryId) {
        danangProductRepository.replicateProduct(id, name, price, categoryId);
    }

    @Transactional(value = "hcmTransactionManager")
    public void replicateToHcm(Integer id, String name, BigDecimal price, Integer categoryId) {
        hcmProductRepository.replicateProduct(id, name, price, categoryId);
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
