package com.example.csdlpt.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.csdlpt.entity.CustomerIdentity;
import com.example.csdlpt.entity.ReplicationLog;
import com.example.csdlpt.enums.ReplicationStatus;
import com.example.csdlpt.repository.site_dn.DanangCustomerIdentityRepository;
import com.example.csdlpt.repository.site_dn.DanangReplicationLogRepository;
import com.example.csdlpt.repository.site_hcm.HcmCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hcm.HcmReplicationLogRepository;
import com.example.csdlpt.repository.site_hn.HanoiCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hn.HanoiReplicationLogRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Xử lý các replication log PENDING của customer_identity tại từng site nguồn.
 *
 * Tách khỏi scheduled job để @Transactional đi qua proxy của Spring, tránh bị
 * bỏ qua do self-invocation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerReplicationProcessor {

    static final String ENTITY_TYPE = "CUSTOMER_IDENTITY";
    static final int MAX_RETRY_COUNT = 5;

    HanoiReplicationLogRepository hanoiLogRepo;
    DanangReplicationLogRepository danangLogRepo;
    HcmReplicationLogRepository hcmLogRepo;

    HanoiCustomerIdentityRepository hanoiIdentityRepo;
    DanangCustomerIdentityRepository danangIdentityRepo;
    HcmCustomerIdentityRepository hcmIdentityRepo;
    CustomerIdentityReplicationWriter replicationWriter;

    @Transactional("hanoiTransactionManager")
    public void processHanoiLogs() {
        List<ReplicationLog> pendingLogs = hanoiLogRepo.findByEntityTypeAndStatus(ENTITY_TYPE, ReplicationStatus.PENDING);
        for (ReplicationLog entry : pendingLogs) {
            processLog(entry, "HN", hanoiIdentityRepo.findById(entry.getEntityId()), hanoiLogRepo::save);
        }
    }

    @Transactional("danangTransactionManager")
    public void processDanangLogs() {
        List<ReplicationLog> pendingLogs = danangLogRepo.findByEntityTypeAndStatus(ENTITY_TYPE, ReplicationStatus.PENDING);
        for (ReplicationLog entry : pendingLogs) {
            processLog(entry, "DN", danangIdentityRepo.findById(entry.getEntityId()), danangLogRepo::save);
        }
    }

    @Transactional("hcmTransactionManager")
    public void processHcmLogs() {
        List<ReplicationLog> pendingLogs = hcmLogRepo.findByEntityTypeAndStatus(ENTITY_TYPE, ReplicationStatus.PENDING);
        for (ReplicationLog entry : pendingLogs) {
            processLog(entry, "HCM", hcmIdentityRepo.findById(entry.getEntityId()), hcmLogRepo::save);
        }
    }

    private void processLog(
            ReplicationLog entry,
            String sourceSite,
            Optional<CustomerIdentity> sourceIdentity,
            LogSaver logSaver) {
        try {
            if (sourceIdentity.isEmpty()) {
                markFailed(entry, "Không tìm thấy entity tại site nguồn " + sourceSite);
                logSaver.save(entry);
                log.warn("[CustomerReplication] Bỏ qua {}->{}: không tìm thấy identity id={}",
                        sourceSite, entry.getTargetSite(), entry.getEntityId());
                return;
            }

            CustomerIdentity identity = sourceIdentity.get();
            replicateTo(identity, entry.getTargetSite());

            entry.setStatus(ReplicationStatus.DONE);
            entry.setErrorMessage(null);
            logSaver.save(entry);
            log.info("[CustomerReplication] Đã nhân bản identity id={} từ {} sang {}",
                    identity.getId(), sourceSite, entry.getTargetSite());
        } catch (Exception e) {
            markRetryOrFailed(entry, e.getMessage());
            logSaver.save(entry);
            log.error("[CustomerReplication] Lỗi nhân bản identity id={} từ {} sang {}: {}",
                    entry.getEntityId(), sourceSite, entry.getTargetSite(), e.getMessage());
        }
    }

    private void replicateTo(CustomerIdentity identity, String targetSite) {
        Integer mainSiteId = identity.getMainSite() != null ? identity.getMainSite().getId() : null;
        switch (targetSite) {
            case "HN" -> replicationWriter.replicateToHanoi(
                    identity.getId(), identity.getEmail(), identity.getPassword(), mainSiteId);
            case "DN" -> replicationWriter.replicateToDanang(
                    identity.getId(), identity.getEmail(), identity.getPassword(), mainSiteId);
            case "HCM" -> replicationWriter.replicateToHcm(
                    identity.getId(), identity.getEmail(), identity.getPassword(), mainSiteId);
            default -> throw new IllegalArgumentException("targetSite không hợp lệ: " + targetSite);
        }
    }

    private void markRetryOrFailed(ReplicationLog logEntry, String errorMessage) {
        int nextRetryCount = (logEntry.getRetryCount() != null ? logEntry.getRetryCount() : 0) + 1;
        logEntry.setRetryCount(nextRetryCount);
        logEntry.setErrorMessage(errorMessage);
        logEntry.setStatus(nextRetryCount >= MAX_RETRY_COUNT ? ReplicationStatus.FAILED : ReplicationStatus.PENDING);
    }

    private void markFailed(ReplicationLog logEntry, String errorMessage) {
        int nextRetryCount = (logEntry.getRetryCount() != null ? logEntry.getRetryCount() : 0) + 1;
        logEntry.setRetryCount(nextRetryCount);
        logEntry.setErrorMessage(errorMessage);
        logEntry.setStatus(ReplicationStatus.FAILED);
    }

    @FunctionalInterface
    private interface LogSaver {
        ReplicationLog save(ReplicationLog logEntry);
    }
}
