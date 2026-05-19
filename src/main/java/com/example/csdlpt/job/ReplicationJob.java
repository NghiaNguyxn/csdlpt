package com.example.csdlpt.job;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.csdlpt.entity.ProductBasic;
import com.example.csdlpt.entity.ReplicationLog;
import com.example.csdlpt.enums.ReplicationStatus;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.site_hn.HanoiProductRepository;
import com.example.csdlpt.repository.site_hn.HanoiReplicationLogRepository;
import com.example.csdlpt.service.ReplicationService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReplicationJob {

    HanoiReplicationLogRepository logRepository;
    HanoiProductRepository hanoiProductRepository;
    ReplicationService replicationService;

    @Scheduled(fixedDelay = 10000)
    public void processPendingLogs() {
        // Job chỉ xử lý replication log ở master HN; test profile tắt job để tránh H2 thiếu schema thật.
        List<ReplicationLog> pendingLogs = logRepository.findByStatusAndTargetSite(ReplicationStatus.PENDING, "DN");
        pendingLogs.addAll(logRepository.findByStatusAndTargetSite(ReplicationStatus.PENDING, "HCM"));

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
}
