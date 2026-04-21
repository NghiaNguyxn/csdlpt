package com.example.csdlpt.job;

import com.example.csdlpt.entity.ProductBasic;
import com.example.csdlpt.entity.ReplicationLog;
import com.example.csdlpt.enums.ReplicationStatus;
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

                    // Lấy dữ liệu gốc từ Master (Hà Nội)
                    ProductBasic masterProduct = hanoiProductRepository.findById(logEntry.getEntityId())
                            .orElseThrow(() -> new RuntimeException(
                                    "Không tìm thấy Product ID=" + logEntry.getEntityId() + " tại Master!"));

                    // Sử dụng Native Query để bảo toàn ID và tránh lỗi Hibernate Session
                    Integer categoryId = masterProduct.getCategory() != null ? masterProduct.getCategory().getId()
                            : null;

                    if ("DN".equals(logEntry.getTargetSite())) {
                        replicationService.replicateToDanang(
                                masterProduct.getId(),
                                masterProduct.getName(),
                                masterProduct.getPrice(),
                                categoryId);
                    } else if ("HCM".equals(logEntry.getTargetSite())) {
                        replicationService.replicateToHcm(
                                masterProduct.getId(),
                                masterProduct.getName(),
                                masterProduct.getPrice(),
                                categoryId);
                    }

                    // Đánh dấu thành công (Transaction tại Master HN)
                    replicationService.updateLogStatus(logEntry, ReplicationStatus.DONE, null);

                    log.info("Đã đồng bộ thành công (Robust Service) Log ID {} sang Site {}", logEntry.getId(),
                            logEntry.getTargetSite());
                }

            } catch (Exception e) {
                log.error("Lỗi khi đồng bộ Log ID {}: {}", logEntry.getId(), e.getMessage());
                // Lưu lại lỗi và tăng retry_count tường minh vào Master DB
                replicationService.updateLogStatus(logEntry, ReplicationStatus.PENDING, e.getMessage());

                // Nếu thử quá 5 lần thì đánh dấu FAILED
                if (logEntry.getRetryCount() >= 5) {
                    replicationService.updateLogStatus(logEntry, ReplicationStatus.FAILED, e.getMessage());
                }
            }
        }

        // Không cần saveAll ở cuối nữa vì đã save lẻ trong từng transaction thành công
    }
}
