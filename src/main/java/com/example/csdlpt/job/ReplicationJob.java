package com.example.csdlpt.job;

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
    ReplicationService replicationService;

    @Scheduled(fixedDelay = 10000) // Chạy 10 giây 1 lần
    public void processPendingLogs() {
        // Lấy tất cả các log đang chờ
        List<ReplicationLog> pendingLogs = logRepository.findByStatusAndTargetSite(ReplicationStatus.PENDING, "DN");
        pendingLogs.addAll(logRepository.findByStatusAndTargetSite(ReplicationStatus.PENDING, "HCM"));

        if (pendingLogs.isEmpty()) {
            return; // Không có gì để đồng bộ
        }

        log.info("Phát hiện {} bản ghi Replication Log cần đồng bộ...", pendingLogs.size());

        for (ReplicationLog logEntry : pendingLogs) {
            try {
                // Chỉ xử lý table PRODUCT
                if ("PRODUCT".equals(logEntry.getEntityType())) {

                    // Lấy dữ liệu gốc từ Master (Hà Nội). 
                    // Chú ý: Dùng findById thay vì findByIdAndIsActiveTrue vì ta muốn đồng bộ cả trạng thái is_active=false (Soft Delete)
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

                    // Đánh dấu thành công
                    replicationService.updateLogStatus(logEntry, ReplicationStatus.DONE, null);

                    log.info("Đã đồng bộ thành công (Action: {}) Log ID {} sang Site {}", 
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
