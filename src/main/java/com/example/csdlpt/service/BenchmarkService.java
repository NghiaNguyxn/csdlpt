package com.example.csdlpt.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.csdlpt.dto.response.BenchmarkMetricResponse;
import com.example.csdlpt.dto.response.GlobalStockBenchmarkResponse;
import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.enums.SiteCode;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BenchmarkService {

    private static final int DEFAULT_ITERATIONS = 100;
    private static final int DEFAULT_WARMUP_ITERATIONS = 10;
    private static final int MAX_ITERATIONS = 10_000;

    InventoryService inventoryService;
    SiteRoutingService siteRoutingService;

    @Qualifier("centralJdbcTemplate")
    JdbcTemplate centralJdbcTemplate;

    public GlobalStockBenchmarkResponse benchmarkGlobalStock(Integer productId, Integer iterations, Integer warmup) {
        if (productId == null || productId <= 0) {
            throw new AppException(ErrorCode.INVALID_KEY, "productId không hợp lệ");
        }

        int actualIterations = normalizeCount(iterations, DEFAULT_ITERATIONS);
        int actualWarmup = normalizeCount(warmup, DEFAULT_WARMUP_ITERATIONS);

        refreshCentralInventory(productId);

        Integer centralizedQuantity = queryCentralizedStock(productId);
        Integer distributedQuantity = inventoryService.getGlobalStock(productId).getQuantity();

        runWarmup(productId, actualWarmup);

        List<Long> centralizedDurations = new ArrayList<>(actualIterations);
        List<Long> distributedDurations = new ArrayList<>(actualIterations);

        for (int i = 0; i < actualIterations; i++) {
            centralizedDurations.add(measureNanos(() -> queryCentralizedStock(productId)));
            distributedDurations.add(measureNanos(() -> inventoryService.getGlobalStock(productId)));
        }

        return GlobalStockBenchmarkResponse.builder()
                .productId(productId)
                .iterations(actualIterations)
                .warmupIterations(actualWarmup)
                .centralizedQuantity(centralizedQuantity)
                .distributedQuantity(distributedQuantity)
                .centralized(toMetric(centralizedDurations))
                .distributed(toMetric(distributedDurations))
                .build();
    }

    private void refreshCentralInventory(Integer productId) {
        centralJdbcTemplate.update("delete from central_inventory where product_id = ?", productId);

        for (SiteCode siteCode : SiteCode.values()) {
            List<Inventory> inventories;
            try {
                inventories = siteRoutingService.findInventoryByProductAndSite(productId, siteCode);
            } catch (RuntimeException ex) {
                log.warn("Không thể đồng bộ tồn kho benchmark từ site {}, productId={}. Bỏ qua site này. Lý do: {}",
                        siteCode, productId, ex.getMessage());
                continue;
            }
            for (Inventory inventory : inventories) {
                centralJdbcTemplate.update("""
                        insert into central_inventory
                            (site_code, warehouse_id, product_id, quantity, reserved_quantity)
                        values (?, ?, ?, ?, ?)
                        """,
                        siteCode.name(),
                        inventory.getId().getWarehouseId(),
                        inventory.getId().getProductId(),
                        inventory.getQuantity() == null ? 0 : inventory.getQuantity(),
                        inventory.getReservedQuantity() == null ? 0 : inventory.getReservedQuantity());
            }
        }
    }

    private void runWarmup(Integer productId, int warmup) {
        for (int i = 0; i < warmup; i++) {
            queryCentralizedStock(productId);
            inventoryService.getGlobalStock(productId);
        }
    }

    private Integer queryCentralizedStock(Integer productId) {
        Integer quantity = centralJdbcTemplate.queryForObject(
                "select coalesce(sum(quantity), 0) from central_inventory where product_id = ?",
                Integer.class,
                productId);
        return quantity == null ? 0 : quantity;
    }

    private long measureNanos(Runnable runnable) {
        long startedAt = System.nanoTime();
        runnable.run();
        return System.nanoTime() - startedAt;
    }

    private BenchmarkMetricResponse toMetric(List<Long> nanos) {
        List<Long> millis = nanos.stream()
                .map(duration -> Math.max(0L, Math.round(duration / 1_000_000.0)))
                .sorted()
                .toList();

        long min = millis.isEmpty() ? 0 : millis.getFirst();
        long max = millis.isEmpty() ? 0 : millis.getLast();
        double avg = nanos.stream()
                .mapToDouble(duration -> duration / 1_000_000.0)
                .average()
                .orElse(0);
        long p95 = percentile(millis, 0.95);

        return BenchmarkMetricResponse.builder()
                .minMs(min)
                .maxMs(max)
                .avgMs(Math.round(avg * 100.0) / 100.0)
                .p95Ms(p95)
                .build();
    }

    private long percentile(List<Long> sortedMillis, double percentile) {
        if (sortedMillis.isEmpty()) {
            return 0;
        }

        int index = (int) Math.ceil(percentile * sortedMillis.size()) - 1;
        index = Math.max(0, Math.min(index, sortedMillis.size() - 1));
        return sortedMillis.get(index);
    }

    private int normalizeCount(Integer value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value <= 0 || value > MAX_ITERATIONS) {
            throw new AppException(ErrorCode.INVALID_KEY,
                    "Số lần benchmark phải nằm trong khoảng 1.." + MAX_ITERATIONS);
        }
        return value;
    }
}
