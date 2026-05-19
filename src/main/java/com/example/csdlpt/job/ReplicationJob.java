package com.example.csdlpt.job;

import com.example.csdlpt.entity.ProductBasic;
import com.example.csdlpt.entity.Category;
import com.example.csdlpt.entity.ReplicationLog;
import com.example.csdlpt.enums.ReplicationStatus;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.site_hn.HanoiProductRepository;
import com.example.csdlpt.repository.site_hn.HanoiCategoryRepository;
import com.example.csdlpt.repository.site_hn.HanoiReplicationLogRepository;
import com.example.csdlpt.service.ReplicationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReplicationJob {

    HanoiReplicationLogRepository logRepository;
    HanoiProductRepository hanoiProductRepository;
    HanoiCategoryRepository hanoiCategoryRepository;
    ReplicationService replicationService;

    @Scheduled(fixedDelay = 10000) 
    public void processPendingLogs() {
        
        List<ReplicationLog> pendingLogs = logRepository.findByStatusAndTargetSite(ReplicationStatus.PENDING, "DN");
        pendingLogs.addAll(logRepository.findByStatusAndTargetSite(ReplicationStatus.PENDING, "HCM"));

        if (pendingLogs.isEmpty()) {
            return; 
        }

        log.info("Phát hiện {} bản ghi Replication Log cần đồng bộ...", pendingLogs.size());

        for (ReplicationLog logEntry : pendingLogs) {
            try {
                if ("PRODUCT".equals(logEntry.getEntityType())) {
                    ProductBasic masterProduct = hanoiProductRepository.findById(logEntry.getEntityId().intValue())
                            .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND,
                                    "Không tìm thấy Product ID=" + logEntry.getEntityId() + " tại Master!"));

                    Integer categoryId = masterProduct.getCategory() != null ? masterProduct.getCategory().getId()
                            : null;

                    if ("DN".equals(logEntry.getTargetSite())) {
                        replicationService.replicateProductToDanang(
                                masterProduct.getId().longValue(),
                                masterProduct.getName(),
                                masterProduct.getPrice(),
                                categoryId,
                                masterProduct.getIsActive());
                    } else if ("HCM".equals(logEntry.getTargetSite())) {
                        replicationService.replicateProductToHcm(
                                masterProduct.getId().longValue(),
                                masterProduct.getName(),
                                masterProduct.getPrice(),
                                categoryId,
                                masterProduct.getIsActive());
                    }
                    replicationService.updateLogStatus(logEntry, ReplicationStatus.DONE, null);
                    log.info("Đã đồng bộ PRODUCT thành công (Action: {}) Log ID {} sang Site {}",
                            logEntry.getAction(), logEntry.getId(), logEntry.getTargetSite());
                } else if ("CATEGORY".equals(logEntry.getEntityType())) {
                    boolean delete = "DELETE".equals(logEntry.getAction());
                    String categoryName = null;
                    if (!delete) {
                        Category masterCategory = hanoiCategoryRepository.findById(logEntry.getEntityId().intValue())
                                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND,
                                        "Không tìm thấy Category ID=" + logEntry.getEntityId() + " tại Master!"));
                        categoryName = masterCategory.getName();
                    }

                    if ("DN".equals(logEntry.getTargetSite())) {
                        replicationService.replicateCategoryToDanang(logEntry.getEntityId().intValue(), categoryName, delete);
                    } else if ("HCM".equals(logEntry.getTargetSite())) {
                        replicationService.replicateCategoryToHcm(logEntry.getEntityId().intValue(), categoryName, delete);
                    }
                    replicationService.updateLogStatus(logEntry, ReplicationStatus.DONE, null);
                    log.info("Đã đồng bộ CATEGORY thành công (Action: {}) Log ID {} sang Site {}",
                            logEntry.getAction(), logEntry.getId(), logEntry.getTargetSite());
                }

            } catch (Exception e) {
                log.error("Lỗi khi đồng bộ Log ID {}: {}", logEntry.getId(), e.getMessage());
                replicationService.updateLogStatus(logEntry, ReplicationStatus.PENDING, e.getMessage());

                if (logEntry.getRetryCount() >= 5) {
                    replicationService.updateLogStatus(logEntry, ReplicationStatus.FAILED, e.getMessage());
                }
            }
        }
    }
}
