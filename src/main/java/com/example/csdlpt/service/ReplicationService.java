package com.example.csdlpt.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.csdlpt.entity.ReplicationLog;
import com.example.csdlpt.enums.ReplicationAction;
import com.example.csdlpt.enums.ReplicationStatus;
import com.example.csdlpt.repository.site_dn.DanangCategoryRepository;
import com.example.csdlpt.repository.site_dn.DanangProductRepository;
import com.example.csdlpt.repository.site_dn.DanangWarehouseRepository;
import com.example.csdlpt.repository.site_hcm.HcmCategoryRepository;
import com.example.csdlpt.repository.site_hcm.HcmProductRepository;
import com.example.csdlpt.repository.site_hcm.HcmWarehouseRepository;
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
    DanangWarehouseRepository danangWarehouseRepository;
    HcmWarehouseRepository hcmWarehouseRepository;
    DanangCategoryRepository danangCategoryRepository;
    HcmCategoryRepository hcmCategoryRepository;
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

    @Transactional(value = "danangTransactionManager")
    public void replicateWarehouseToDanang(Long id, String code, String name, String location, String region, Integer siteId) {
        danangWarehouseRepository.upsertWarehouse(id.intValue(), code, name, location, region, siteId);
        danangWarehouseRepository.syncWarehouseSequence();
    }

    @Transactional(value = "hcmTransactionManager")
    public void replicateWarehouseToHcm(Long id, String code, String name, String location, String region, Integer siteId) {
        hcmWarehouseRepository.upsertWarehouse(id.intValue(), code, name, location, region, siteId);
        hcmWarehouseRepository.syncWarehouseSequence();
    }

    @Transactional(value = "danangTransactionManager")
    public void deleteWarehouseFromDanang(Long id) {
        if (danangWarehouseRepository.existsById(id.intValue())) {
            danangWarehouseRepository.deleteById(id.intValue());
        }
    }

    @Transactional(value = "hcmTransactionManager")
    public void deleteWarehouseFromHcm(Long id) {
        if (hcmWarehouseRepository.existsById(id.intValue())) {
            hcmWarehouseRepository.deleteById(id.intValue());
        }
    }


    @Transactional(value = "danangTransactionManager")
    public void replicateCategoryToDanang(Long id, String name) {
        danangCategoryRepository.upsertCategory(id.intValue(), name);
    }

    @Transactional(value = "hcmTransactionManager")
    public void replicateCategoryToHcm(Long id, String name) {
        hcmCategoryRepository.upsertCategory(id.intValue(), name);
    }

    @Transactional(value = "danangTransactionManager")
    public void deleteCategoryFromDanang(Long id) {
        if (danangCategoryRepository.existsById(id.intValue())) {
            danangCategoryRepository.deleteById(id.intValue());
        }
    }

    @Transactional(value = "hcmTransactionManager")
    public void deleteCategoryFromHcm(Long id) {
        if (hcmCategoryRepository.existsById(id.intValue())) {
            hcmCategoryRepository.deleteById(id.intValue());
        }
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
