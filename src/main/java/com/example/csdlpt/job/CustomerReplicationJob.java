package com.example.csdlpt.job;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
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
 * Lazy Distributed Replication Job cho CustomerIdentity.
 *
 * Mỗi 15 giây, job quét replication_log tại cả 3 site để tìm các bản ghi PENDING
 * của entity type CUSTOMER_IDENTITY, sau đó đẩy dữ liệu sang site đích tương ứng.
 *
 * Khác với ReplicationJob của product (chỉ đọc log từ HN), CustomerReplicationJob
 * đọc log từ cả 3 site vì customer có thể được tạo tại bất kỳ site nào.
 */
@Component
@Profile("!test")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerReplicationJob {

    // Replication log repositories — đọc PENDING logs từ từng site
    HanoiReplicationLogRepository hanoiLogRepo;
    DanangReplicationLogRepository danangLogRepo;
    HcmReplicationLogRepository hcmLogRepo;

    // Identity repositories — đọc nguồn + ghi đích
    HanoiCustomerIdentityRepository hanoiIdentityRepo;
    DanangCustomerIdentityRepository danangIdentityRepo;
    HcmCustomerIdentityRepository hcmIdentityRepo;

    static final String ENTITY_TYPE = "CUSTOMER_IDENTITY";

    @Scheduled(fixedDelay = 15000)
    public void processCustomerReplication() {
        log.debug("[CustomerReplicationJob] Bắt đầu xử lý lazy replication cho CUSTOMER_IDENTITY");

        // Log tại HN → customer được tạo tại HN, cần đồng bộ sang site đích
        processHanoiLogs();

        // Log tại DN → customer được tạo tại DN, cần đồng bộ sang site đích
        processDanangLogs();

        // Log tại HCM → customer được tạo tại HCM, cần đồng bộ sang site đích
        processHcmLogs();
    }

    // Xử lý log từ HN (source = HN) 
    @Transactional("hanoiTransactionManager")
    public void processHanoiLogs() {
        List<ReplicationLog> pendingLogs = hanoiLogRepo.findByEntityTypeAndStatus(ENTITY_TYPE, ReplicationStatus.PENDING);
        for (ReplicationLog entry : pendingLogs) {
            try {
                Optional<CustomerIdentity> opt = hanoiIdentityRepo.findById(entry.getEntityId());
                if (opt.isEmpty()) {
                    log.warn("[CustomerReplicationJob] HN: Không tìm thấy identity id={}", entry.getEntityId());
                    markFailed(entry, "Entity không tìm thấy tại HN source");
                    hanoiLogRepo.save(entry);
                    continue;
                }
                CustomerIdentity identity = opt.get();
                replicateTo(identity, entry.getTargetSite());

                entry.setStatus(ReplicationStatus.DONE);
                hanoiLogRepo.save(entry);
                log.info("[CustomerReplicationJob] HN→{}: identity id={} đã replicate thành công",
                        entry.getTargetSite(), identity.getId());
            } catch (Exception e) {
                log.error("[CustomerReplicationJob] HN→{}: Lỗi khi replicate id={}: {}",
                        entry.getTargetSite(), entry.getEntityId(), e.getMessage());
                markFailed(entry, e.getMessage());
                hanoiLogRepo.save(entry);
            }
        }
    }

    // Xử lý log từ DN (source = DN) 

    @Transactional("danangTransactionManager")
    public void processDanangLogs() {
        List<ReplicationLog> pendingLogs = danangLogRepo.findByEntityTypeAndStatus(ENTITY_TYPE, ReplicationStatus.PENDING);
        for (ReplicationLog entry : pendingLogs) {
            try {
                Optional<CustomerIdentity> opt = danangIdentityRepo.findById(entry.getEntityId());
                if (opt.isEmpty()) {
                    log.warn("[CustomerReplicationJob] DN: Không tìm thấy identity id={}", entry.getEntityId());
                    markFailed(entry, "Entity không tìm thấy tại DN source");
                    danangLogRepo.save(entry);
                    continue;
                }
                CustomerIdentity identity = opt.get();
                replicateTo(identity, entry.getTargetSite());

                entry.setStatus(ReplicationStatus.DONE);
                danangLogRepo.save(entry);
                log.info("[CustomerReplicationJob] DN→{}: identity id={} đã replicate thành công",
                        entry.getTargetSite(), identity.getId());
            } catch (Exception e) {
                log.error("[CustomerReplicationJob] DN→{}: Lỗi khi replicate id={}: {}",
                        entry.getTargetSite(), entry.getEntityId(), e.getMessage());
                markFailed(entry, e.getMessage());
                danangLogRepo.save(entry);
            }
        }
    }

    // Xử lý log từ HCM (source = HCM) 

    @Transactional("hcmTransactionManager")
    public void processHcmLogs() {
        List<ReplicationLog> pendingLogs = hcmLogRepo.findByEntityTypeAndStatus(ENTITY_TYPE, ReplicationStatus.PENDING);
        for (ReplicationLog entry : pendingLogs) {
            try {
                Optional<CustomerIdentity> opt = hcmIdentityRepo.findById(entry.getEntityId());
                if (opt.isEmpty()) {
                    log.warn("[CustomerReplicationJob] HCM: Không tìm thấy identity id={}", entry.getEntityId());
                    markFailed(entry, "Entity không tìm thấy tại HCM source");
                    hcmLogRepo.save(entry);
                    continue;
                }
                CustomerIdentity identity = opt.get();
                replicateTo(identity, entry.getTargetSite());

                entry.setStatus(ReplicationStatus.DONE);
                hcmLogRepo.save(entry);
                log.info("[CustomerReplicationJob] HCM→{}: identity id={} đã replicate thành công",
                        entry.getTargetSite(), identity.getId());
            } catch (Exception e) {
                log.error("[CustomerReplicationJob] HCM→{}: Lỗi khi replicate id={}: {}",
                        entry.getTargetSite(), entry.getEntityId(), e.getMessage());
                markFailed(entry, e.getMessage());
                hcmLogRepo.save(entry);
            }
        }
    }

    // Upsert vào site đích

    private void replicateTo(CustomerIdentity identity, String targetSite) {
        Integer mainSiteId = identity.getMainSite() != null ? identity.getMainSite().getId() : null;
        switch (targetSite) {
            case "HN" -> hanoiIdentityRepo.replicateCustomerIdentity(
                    identity.getId(), identity.getEmail(), identity.getPassword(), mainSiteId);
            case "DN" -> danangIdentityRepo.replicateCustomerIdentity(
                    identity.getId(), identity.getEmail(), identity.getPassword(), mainSiteId);
            case "HCM" -> hcmIdentityRepo.replicateCustomerIdentity(
                    identity.getId(), identity.getEmail(), identity.getPassword(), mainSiteId);
            default -> throw new IllegalArgumentException("targetSite không hợp lệ: " + targetSite);
        }
    }

    private void markFailed(ReplicationLog log, String errorMessage) {
        log.setStatus(ReplicationStatus.FAILED);
        log.setRetryCount(log.getRetryCount() != null ? log.getRetryCount() + 1 : 1);
        log.setErrorMessage(errorMessage);
    }
}
