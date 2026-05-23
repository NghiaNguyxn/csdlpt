package com.example.csdlpt.service.Customer;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.csdlpt.dto.request.CustomerRequest;
import com.example.csdlpt.entity.CustomerIdentity;
import com.example.csdlpt.entity.CustomerProfile;
import com.example.csdlpt.entity.ReplicationLog;
import com.example.csdlpt.enums.ReplicationStatus;
import com.example.csdlpt.repository.site_dn.DanangCustomerIdentityRepository;
import com.example.csdlpt.repository.site_dn.DanangCustomerProfileRepository;
import com.example.csdlpt.repository.site_dn.DanangReplicationLogRepository;
import com.example.csdlpt.repository.site_hcm.HcmCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hcm.HcmCustomerProfileRepository;
import com.example.csdlpt.repository.site_hcm.HcmReplicationLogRepository;
import com.example.csdlpt.repository.site_hn.HanoiCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hn.HanoiCustomerProfileRepository;
import com.example.csdlpt.repository.site_hn.HanoiReplicationLogRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper bean tách biệt để Spring AOP proxy có thể áp dụng đúng @Transactional
 * theo từng datasource khi tạo customer_identity tại bất kỳ site nào.
 *
 * Lazy distributed protocol: sau khi ghi identity tại site nguồn, ghi log replication
 * cho 2 site còn lại → CustomerReplicationJob sẽ xử lý sau.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerIdentityCreationHelper {

    HanoiCustomerIdentityRepository hanoiIdentityRepo;
    DanangCustomerIdentityRepository danangIdentityRepo;
    HcmCustomerIdentityRepository hcmIdentityRepo;
    HanoiCustomerProfileRepository hanoiProfileRepo;
    DanangCustomerProfileRepository danangProfileRepo;
    HcmCustomerProfileRepository hcmProfileRepo;

    HanoiReplicationLogRepository hanoiLogRepo;
    DanangReplicationLogRepository danangLogRepo;
    HcmReplicationLogRepository hcmLogRepo;

    // Tạo tại HN (source = HN) → log replication sang DN + HCM 

    @Transactional("hanoiTransactionManager")
    public CustomerIdentity createAndLogAtHanoi(CustomerIdentity identity) {
        CustomerIdentity saved = hanoiIdentityRepo.save(identity);
        hanoiLogRepo.saveAll(List.of(
                buildLog(saved.getId(), "DN"),
                buildLog(saved.getId(), "HCM")));
        log.info("[Lazy Replication] Tạo identity id={} tại HN, đã ghi log → DN, HCM", saved.getId());
        return saved;
    }

    // Tạo tại DN (source = DN) → log replication sang HN + HCM 

    @Transactional("danangTransactionManager")
    public CustomerIdentity createAndLogAtDanang(CustomerIdentity identity) {
        CustomerIdentity saved = danangIdentityRepo.save(identity);
        danangLogRepo.saveAll(List.of(
                buildLog(saved.getId(), "HN"),
                buildLog(saved.getId(), "HCM")));
        log.info("[Lazy Replication] Tạo identity id={} tại DN, đã ghi log → HN, HCM", saved.getId());
        return saved;
    }

    // Tạo tại HCM (source = HCM) → log replication sang HN + DN 

    @Transactional("hcmTransactionManager")
    public CustomerIdentity createAndLogAtHcm(CustomerIdentity identity) {
        CustomerIdentity saved = hcmIdentityRepo.save(identity);
        hcmLogRepo.saveAll(List.of(
                buildLog(saved.getId(), "HN"),
                buildLog(saved.getId(), "DN")));
        log.info("[Lazy Replication] Tạo identity id={} tại HCM, đã ghi log → HN, DN", saved.getId());
        return saved;
    }

    @Transactional("hanoiTransactionManager")
    public CustomerProfile createCustomerAtHanoi(CustomerIdentity identity, CustomerRequest request) {
        CustomerIdentity saved = hanoiIdentityRepo.save(identity);
        hanoiLogRepo.saveAll(List.of(
                buildLog(saved.getId(), "DN"),
                buildLog(saved.getId(), "HCM")));
        hanoiProfileRepo.upsertProfile(saved.getId(), request.getName(), request.getPhone(), request.getAddress());
        CustomerProfile profile = hanoiProfileRepo.findById(saved.getId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy customer_profile vừa tạo tại HN"));
        log.info("[Lazy Replication] Tạo customer id={} tại HN, đã ghi log → DN, HCM", saved.getId());
        return profile;
    }

    @Transactional("danangTransactionManager")
    public CustomerProfile createCustomerAtDanang(CustomerIdentity identity, CustomerRequest request) {
        CustomerIdentity saved = danangIdentityRepo.save(identity);
        danangLogRepo.saveAll(List.of(
                buildLog(saved.getId(), "HN"),
                buildLog(saved.getId(), "HCM")));
        danangProfileRepo.upsertProfile(saved.getId(), request.getName(), request.getPhone(), request.getAddress());
        CustomerProfile profile = danangProfileRepo.findById(saved.getId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy customer_profile vừa tạo tại DN"));
        log.info("[Lazy Replication] Tạo customer id={} tại DN, đã ghi log → HN, HCM", saved.getId());
        return profile;
    }

    @Transactional("hcmTransactionManager")
    public CustomerProfile createCustomerAtHcm(CustomerIdentity identity, CustomerRequest request) {
        CustomerIdentity saved = hcmIdentityRepo.save(identity);
        hcmLogRepo.saveAll(List.of(
                buildLog(saved.getId(), "HN"),
                buildLog(saved.getId(), "DN")));
        hcmProfileRepo.upsertProfile(saved.getId(), request.getName(), request.getPhone(), request.getAddress());
        CustomerProfile profile = hcmProfileRepo.findById(saved.getId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy customer_profile vừa tạo tại HCM"));
        log.info("[Lazy Replication] Tạo customer id={} tại HCM, đã ghi log → HN, DN", saved.getId());
        return profile;
    }

    // Cập nhật tại HN 

    @Transactional("hanoiTransactionManager")
    public CustomerIdentity updateAndLogAtHanoi(CustomerIdentity identity) {
        CustomerIdentity saved = hanoiIdentityRepo.save(identity);
        hanoiLogRepo.saveAll(List.of(
                buildUpdateLog(saved.getId(), "DN"),
                buildUpdateLog(saved.getId(), "HCM")));
        log.info("[Lazy Replication] Cập nhật identity id={} tại HN, đã ghi log → DN, HCM", saved.getId());
        return saved;
    }

    @Transactional("danangTransactionManager")
    public CustomerIdentity updateAndLogAtDanang(CustomerIdentity identity) {
        CustomerIdentity saved = danangIdentityRepo.save(identity);
        danangLogRepo.saveAll(List.of(
                buildUpdateLog(saved.getId(), "HN"),
                buildUpdateLog(saved.getId(), "HCM")));
        log.info("[Lazy Replication] Cập nhật identity id={} tại DN, đã ghi log → HN, HCM", saved.getId());
        return saved;
    }

    @Transactional("hcmTransactionManager")
    public CustomerIdentity updateAndLogAtHcm(CustomerIdentity identity) {
        CustomerIdentity saved = hcmIdentityRepo.save(identity);
        hcmLogRepo.saveAll(List.of(
                buildUpdateLog(saved.getId(), "HN"),
                buildUpdateLog(saved.getId(), "DN")));
        log.info("[Lazy Replication] Cập nhật identity id={} tại HCM, đã ghi log → HN, DN", saved.getId());
        return saved;
    }

    // Helpers 

    private ReplicationLog buildLog(Long entityId, String targetSite) {
        return ReplicationLog.builder()
                .entityId(entityId)
                .entityType("CUSTOMER_IDENTITY")
                .action("INSERT")
                .targetSite(targetSite)
                .status(ReplicationStatus.PENDING)
                .build();
    }

    private ReplicationLog buildUpdateLog(Long entityId, String targetSite) {
        return ReplicationLog.builder()
                .entityId(entityId)
                .entityType("CUSTOMER_IDENTITY")
                .action("UPDATE")
                .targetSite(targetSite)
                .status(ReplicationStatus.PENDING)
                .build();
    }

}
