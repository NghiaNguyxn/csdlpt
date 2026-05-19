package com.example.csdlpt.service;

import com.example.csdlpt.dto.request.RaceConditionRequest;
import com.example.csdlpt.dto.response.RaceConditionResponse;
import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.entity.InventoryId;
import com.example.csdlpt.entity.ProductBasic;
import com.example.csdlpt.entity.Warehouse;
import com.example.csdlpt.enums.SiteCode;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.common.DistributedInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class RaceConditionTestService {
    private final SiteRoutingService siteRoutingService;

    public RaceConditionResponse runRaceConditionTest(RaceConditionRequest request) {
        SiteCode siteCode = parseSite(defaultString(request.getSiteCode(), "HN"));
        Integer warehouseId = request.getWarehouseId() == null ? 1 : request.getWarehouseId();
        Integer productId = request.getProductId() == null ? 1 : request.getProductId();
        Integer initialQuantity = request.getInitialQuantity() == null ? 1 : request.getInitialQuantity();
        Integer quantityPerOrder = request.getQuantityPerOrder() == null ? 1 : request.getQuantityPerOrder();
        Integer threadCount = request.getThreadCount() == null ? 2 : request.getThreadCount();

        ProductBasic product = siteRoutingService.findProductBySite(productId, siteCode);
        Warehouse warehouse = siteRoutingService.findWarehouseBySite(warehouseId, siteCode);
        DistributedInventoryRepository inventoryRepo = siteRoutingService.getInventoryRepository(siteCode);
        InventoryId inventoryId = InventoryId.builder()
                .warehouseId(warehouse.getId())
                .productId(product.getId())
                .build();

        Inventory inventory = inventoryRepo.findById(inventoryId)
                .orElseGet(() -> Inventory.builder()
                        .id(inventoryId)
                        .product(product)
                        .warehouse(warehouse)
                        .quantity(0)
                        .build());
        inventory.setQuantity(initialQuantity);
        inventoryRepo.save(inventory);

        List<String> logs = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 1; i <= threadCount; i++) {
            final int threadNo = i;
            executor.submit(() -> {
                try {
                    logs.add("Thread-" + threadNo + " READY");
                    startGate.await();
                    int updated = inventoryRepo.reduceStockIfEnough(warehouseId, productId, quantityPerOrder);
                    if (updated == 1) {
                        success.incrementAndGet();
                        logs.add("Thread-" + threadNo + " SUCCESS: trừ tồn kho thành công");
                    } else {
                        failed.incrementAndGet();
                        logs.add("Thread-" + threadNo + " FAILED: không đủ tồn kho");
                    }
                } catch (Exception e) {
                    failed.incrementAndGet();
                    logs.add("Thread-" + threadNo + " ERROR: " + e.getMessage());
                }
            });
        }

        startGate.countDown();
        executor.shutdown();
        try {
            executor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.UNCATEGORIED_EXCEPTION, "Race condition test bị gián đoạn");
        }

        Integer finalQuantity = inventoryRepo.findQuantityNative(warehouseId, productId);
        logs.add("FINAL_QUANTITY=" + finalQuantity);

        return RaceConditionResponse.builder()
                .siteCode(siteCode.name())
                .productId(productId)
                .initialQuantity(initialQuantity)
                .finalQuantity(finalQuantity)
                .successCount(success.get())
                .failedCount(failed.get())
                .logs(new ArrayList<>(logs))
                .build();
    }

    private SiteCode parseSite(String siteCode) {
        try {
            return SiteCode.valueOf(siteCode.trim().toUpperCase());
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_KEY, "SiteCode không hợp lệ: " + siteCode);
        }
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
