package com.example.csdlpt.service.Customer;

import org.springframework.stereotype.Component;

import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;

/**
 * Sinh BIGINT ID có prefix theo site để giảm rủi ro trùng khóa khi tạo khách hàng
 * ở nhiều site trong mô hình lazy replication.
 *
 * Dải ID:
 * - HN:  [1_000_000_000_000_000, 2_000_000_000_000_000)
 * - DN:  [2_000_000_000_000_000, 3_000_000_000_000_000)
 * - HCM: [3_000_000_000_000_000, 4_000_000_000_000_000)
 */
@Component
public class CustomerIdGenerator {

    private static final long SITE_RANGE = 1_000_000_000_000_000L;
    private static final long MILLIS_MOD = 1_000_000_000_000L;
    private static final int MAX_SEQUENCE_PER_MILLIS = 1_000;

    private long lastMillis = -1L;
    private int sequence = 0;

    public synchronized Long generate(Integer siteId) {
        validateSiteId(siteId);

        long now = System.currentTimeMillis();
        if (now == lastMillis) {
            sequence = (sequence + 1) % MAX_SEQUENCE_PER_MILLIS;
            if (sequence == 0) {
                now = waitNextMillis(now);
            }
        } else {
            sequence = 0;
        }
        lastMillis = now;

        long suffix = (now % MILLIS_MOD) * MAX_SEQUENCE_PER_MILLIS + sequence;
        return siteId * SITE_RANGE + suffix;
    }

    private void validateSiteId(Integer siteId) {
        if (siteId == null || siteId < 1 || siteId > 3) {
            throw new AppException(ErrorCode.INVALID_KEY, "mainSiteId chỉ nhận 1=HN, 2=DN, 3=HCM");
        }
    }

    private long waitNextMillis(long currentMillis) {
        long now = System.currentTimeMillis();
        while (now <= currentMillis) {
            now = System.currentTimeMillis();
        }
        return now;
    }
}
