package com.example.csdlpt.job;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.csdlpt.entity.ProductBasic;
import com.example.csdlpt.entity.ReplicationLog;
import com.example.csdlpt.entity.Warehouse;
import com.example.csdlpt.enums.ReplicationStatus;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.site_hn.HanoiCategoryRepository;
import com.example.csdlpt.repository.site_hn.HanoiProductRepository;
import com.example.csdlpt.repository.site_hn.HanoiReplicationLogRepository;
import com.example.csdlpt.repository.site_hn.HanoiWarehouseRepository;
import com.example.csdlpt.service.ReplicationService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReplicationJob {

    HanoiReplicationLogRepository logRepository;
    HanoiProductRepository hanoiProductRepository;
    HanoiWarehouseRepository hanoiWarehouseRepository;
    HanoiCategoryRepository hanoiCategoryRepository;
    ReplicationService replicationService;

    @Scheduled(fixedDelay = 10000)
    public void processPendingLogs() {
        // Job chỉ xử lý replication log ở master HN.
        List<ReplicationLog> pendingLogs = logRepository.findByStatusAndTargetSiteOrderByIdAsc(ReplicationStatus.PENDING, "DN");
        pendingLogs.addAll(logRepository.findByStatusAndTargetSiteOrderByIdAsc(ReplicationStatus.PENDING, "HCM"));

        if (pendingLogs.isEmpty()) {
            return;
        }

        log.info("Phát hiện {} bản ghi replication cần đồng bộ", pendingLogs.size());

        for (ReplicationLog logEntry : pendingLogs) {
            try {
                if ("PRODUCT".equals(logEntry.getEntityType())) {
                    replicateProduct(logEntry);
                    replicationService.markLogDone(logEntry);

                    log.info("Đã đồng bộ thành công action={} logId={} sang site={}",
                            logEntry.getAction(), logEntry.getId(), logEntry.getTargetSite());
                } else if ("WAREHOUSE".equals(logEntry.getEntityType())) {
                    replicateWarehouse(logEntry);
                    replicationService.markLogDone(logEntry);

                    log.info("Đã đồng bộ warehouse thành công action={} logId={} sang site={}",
                            logEntry.getAction(), logEntry.getId(), logEntry.getTargetSite());
                } else if ("CATEGORY".equals(logEntry.getEntityType())) {
                    replicateCategory(logEntry);
                    replicationService.markLogDone(logEntry);

                    log.info("Đã đồng bộ category thành công action={} logId={} sang site={}",
                            logEntry.getAction(), logEntry.getId(), logEntry.getTargetSite());
                }
            } catch (Exception e) {
                log.error("Lỗi khi đồng bộ logId={}: {}", logEntry.getId(), e.getMessage());
                replicationService.markLogRetry(logEntry, e.getMessage());

                // Sau 5 lần retry không thành công, đánh dấu FAILED để không retry vô hạn.
                if (logEntry.getRetryCount() >= 5) {
                    replicationService.markLogFailed(logEntry, e.getMessage());
                }
            }
        }
    }

    private void replicateProduct(ReplicationLog logEntry) {
        // Dùng findById để replicate được cả bản ghi đã soft delete với is_active=false.
        ProductBasic masterProduct = hanoiProductRepository.findById(logEntry.getEntityId().intValue())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND,
                        "Không tìm thấy productId=" + logEntry.getEntityId() + " tại master HN"));

        Integer categoryId = masterProduct.getCategory() != null ? masterProduct.getCategory().getId() : null;

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
    }

    private void replicateWarehouse(ReplicationLog logEntry) {
        if ("DELETE".equals(logEntry.getAction())) {
            if ("DN".equals(logEntry.getTargetSite())) {
                replicationService.deleteWarehouseFromDanang(logEntry.getEntityId());
            } else if ("HCM".equals(logEntry.getTargetSite())) {
                replicationService.deleteWarehouseFromHcm(logEntry.getEntityId());
            }
            return;
        }

        Warehouse masterWarehouse = hanoiWarehouseRepository.findById(logEntry.getEntityId().intValue())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND,
                        "Không tìm thấy warehouseId=" + logEntry.getEntityId() + " tại master HN"));

        Integer siteId = masterWarehouse.getSite() != null ? masterWarehouse.getSite().getId() : null;

        if ("DN".equals(logEntry.getTargetSite())) {
            replicationService.replicateWarehouseToDanang(
                    masterWarehouse.getId().longValue(),
                    masterWarehouse.getCode(),
                    masterWarehouse.getName(),
                    masterWarehouse.getLocation(),
                    masterWarehouse.getRegion(),
                    siteId);
        } else if ("HCM".equals(logEntry.getTargetSite())) {
            replicationService.replicateWarehouseToHcm(
                    masterWarehouse.getId().longValue(),
                    masterWarehouse.getCode(),
                    masterWarehouse.getName(),
                    masterWarehouse.getLocation(),
                    masterWarehouse.getRegion(),
                    siteId);
        }
    }


    private void replicateCategory(ReplicationLog logEntry) {
        if ("DELETE".equals(logEntry.getAction())) {
            if ("DN".equals(logEntry.getTargetSite())) {
                replicationService.deleteCategoryFromDanang(logEntry.getEntityId());
            } else if ("HCM".equals(logEntry.getTargetSite())) {
                replicationService.deleteCategoryFromHcm(logEntry.getEntityId());
            }
            return;
        }

        Category masterCategory = hanoiCategoryRepository.findById(logEntry.getEntityId().intValue())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND,
                        "Không tìm thấy categoryId=" + logEntry.getEntityId() + " tại master HN"));

        if ("DN".equals(logEntry.getTargetSite())) {
            replicationService.replicateCategoryToDanang(
                    masterCategory.getId().longValue(),
                    masterCategory.getName());
        } else if ("HCM".equals(logEntry.getTargetSite())) {
            replicationService.replicateCategoryToHcm(
                    masterCategory.getId().longValue(),
                    masterCategory.getName());
        }
    }

}
