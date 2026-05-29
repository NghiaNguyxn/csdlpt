package com.example.csdlpt.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.csdlpt.service.Customer.CustomerReplicationProcessor;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Bộ lập lịch nhân bản lười cho CustomerIdentity.
 *
 * Phần xử lý có transaction được ủy quyền sang CustomerReplicationProcessor để
 * Spring AOP áp dụng đúng transaction manager cho từng datasource.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerReplicationJob {

    CustomerReplicationProcessor customerReplicationProcessor;

    @Scheduled(fixedDelay = 15000)
    public void processCustomerReplication() {
        log.debug("[CustomerReplicationJob] Bắt đầu xử lý lazy replication cho CUSTOMER_IDENTITY");

        processSite("HN", customerReplicationProcessor::processHanoiLogs);
        processSite("DN", customerReplicationProcessor::processDanangLogs);
        processSite("HCM", customerReplicationProcessor::processHcmLogs);
    }

    private void processSite(String siteCode, Runnable replicationTask) {
        try {
            replicationTask.run();
        } catch (RuntimeException ex) {
            log.warn("[CustomerReplicationJob] Bỏ qua lần chạy replication tại site {} vì site không hoạt động hoặc không truy vấn được: {}",
                    siteCode, ex.getMessage());
        }
    }
}
