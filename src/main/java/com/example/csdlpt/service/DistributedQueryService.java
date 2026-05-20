package com.example.csdlpt.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.example.csdlpt.dto.response.DistributedOrderLineResponse;
import com.example.csdlpt.dto.response.MultiWarehouseOrderResponse;
import com.example.csdlpt.dto.response.QueryPerformanceResponse;
import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.repository.MultiWarehouseOrderLineProjection;
import com.example.csdlpt.repository.site_dn.DanangInventoryRepository;
import com.example.csdlpt.repository.site_dn.DanangOrderDetailRepository;
import com.example.csdlpt.repository.site_hcm.HcmInventoryRepository;
import com.example.csdlpt.repository.site_hcm.HcmOrderDetailRepository;
import com.example.csdlpt.repository.site_hn.HanoiInventoryRepository;
import com.example.csdlpt.repository.site_hn.HanoiOrderDetailRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DistributedQueryService {

    HanoiOrderDetailRepository hanoiOrderDetailRepository;
    DanangOrderDetailRepository danangOrderDetailRepository;
    HcmOrderDetailRepository hcmOrderDetailRepository;
    HanoiInventoryRepository hanoiInventoryRepository;
    DanangInventoryRepository danangInventoryRepository;
    HcmInventoryRepository hcmInventoryRepository;

    public List<MultiWarehouseOrderResponse> findOrdersExportedFromMultipleWarehouses() {
        List<DistributedOrderLineResponse> allLines = Stream.of(
                hanoiOrderDetailRepository.findDistributedOrderLines(),
                danangOrderDetailRepository.findDistributedOrderLines(),
                hcmOrderDetailRepository.findDistributedOrderLines())
                .flatMap(List::stream)
                .map(this::toLineResponse)
                .toList();

        Map<Long, List<DistributedOrderLineResponse>> linesByOrder = allLines.stream()
                .collect(Collectors.groupingBy(DistributedOrderLineResponse::getOrderId));

        return linesByOrder.entrySet().stream()
                .map(entry -> toMultiWarehouseOrder(entry.getKey(), entry.getValue()))
                .filter(response -> response.getWarehouseCount() > 1)
                .sorted(Comparator.comparing(MultiWarehouseOrderResponse::getOrderId))
                .toList();
    }

    public QueryPerformanceResponse compareQ1CentralizedAndDistributed(Integer productId) {
        final int ITERATIONS = 5;

        // === WARM-UP: khởi tạo connection pool, query plan cache ===
        hanoiInventoryRepository.findByProductId(productId);
        danangInventoryRepository.findByProductId(productId);
        hcmInventoryRepository.findByProductId(productId);
        hanoiInventoryRepository.sumQuantityByProductId(productId);
        danangInventoryRepository.sumQuantityByProductId(productId);
        hcmInventoryRepository.sumQuantityByProductId(productId);

        // === ĐO XEN KẼ: C → D → C → D → ... để loại bỏ ordering bias ===
        long[] centralizedSamples = new long[ITERATIONS];
        long[] distributedSamples = new long[ITERATIONS];
        List<Inventory> centralizedRows = new ArrayList<>();
        int centralizedQuantity = 0;
        int distributedQuantity = 0;

        for (int i = 0; i < ITERATIONS; i++) {
            // Centralized turn
            centralizedRows.clear();
            long cStart = System.nanoTime();
            centralizedRows.addAll(hanoiInventoryRepository.findByProductId(productId));
            centralizedRows.addAll(danangInventoryRepository.findByProductId(productId));
            centralizedRows.addAll(hcmInventoryRepository.findByProductId(productId));
            centralizedQuantity = centralizedRows.stream().map(Inventory::getQuantity).reduce(0, Integer::sum);
            centralizedSamples[i] = System.nanoTime() - cStart;

            // Distributed turn
            long dStart = System.nanoTime();
            int hn = hanoiInventoryRepository.sumQuantityByProductId(productId);
            int dn = danangInventoryRepository.sumQuantityByProductId(productId);
            int hcm = hcmInventoryRepository.sumQuantityByProductId(productId);
            distributedQuantity = hn + dn + hcm;
            distributedSamples[i] = System.nanoTime() - dStart;
        }

        // Lấy trung bình (bỏ sample đầu tiên vì vẫn có thể còn JIT bias)
        long centralizedElapsed = 0;
        long distributedElapsed = 0;
        for (int i = 1; i < ITERATIONS; i++) {
            centralizedElapsed += centralizedSamples[i];
            distributedElapsed += distributedSamples[i];
        }
        centralizedElapsed /= (ITERATIONS - 1);
        distributedElapsed /= (ITERATIONS - 1);

        log.info("Q6 productId={}, centralized avg={}ns, distributed avg={}ns (over {} iterations)",
                productId, centralizedElapsed, distributedElapsed, ITERATIONS - 1);

        return QueryPerformanceResponse.builder()
                .productId(productId)
                .centralizedQuantity(centralizedQuantity)
                .distributedQuantity(distributedQuantity)
                .centralizedElapsedNanos(centralizedElapsed)
                .distributedElapsedNanos(distributedElapsed)
                .centralizedElapsedMillis(toMillis(centralizedElapsed))
                .distributedElapsedMillis(toMillis(distributedElapsed))
                .centralizedRowsTransferred(centralizedRows.size())
                .distributedRowsTransferred(3)
                .iterations(ITERATIONS - 1)
                .centralizedStrategy("Centralized simulation: transfer inventory rows to coordinator, then sum")
                .distributedStrategy("Distributed Q1: each site computes SUM(quantity), coordinator transfers only 3 partial totals")
                .build();
    }

    private DistributedOrderLineResponse toLineResponse(MultiWarehouseOrderLineProjection projection) {
        return DistributedOrderLineResponse.builder()
                .orderId(projection.getOrderId())
                .siteCode(projection.getSiteCode())
                .warehouseId(projection.getWarehouseId())
                .warehouseCode(projection.getWarehouseCode())
                .warehouseName(projection.getWarehouseName())
                .productId(projection.getProductId())
                .productName(projection.getProductName())
                .quantity(projection.getQuantity())
                .price(projection.getPrice())
                .build();
    }

    private MultiWarehouseOrderResponse toMultiWarehouseOrder(Long orderId, List<DistributedOrderLineResponse> lines) {
        Set<String> warehouseKeys = lines.stream()
                .map(line -> line.getSiteCode() + ":" + line.getWarehouseId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> siteCodes = lines.stream()
                .map(DistributedOrderLineResponse::getSiteCode)
                .distinct()
                .sorted()
                .toList();

        List<String> warehouseCodes = lines.stream()
                .map(DistributedOrderLineResponse::getWarehouseCode)
                .distinct()
                .sorted()
                .toList();

        return MultiWarehouseOrderResponse.builder()
                .orderId(orderId)
                .warehouseCount(warehouseKeys.size())
                .siteCodes(siteCodes)
                .warehouseCodes(warehouseCodes)
                .lines(lines.stream()
                        .sorted(Comparator.comparing(DistributedOrderLineResponse::getSiteCode)
                                .thenComparing(DistributedOrderLineResponse::getWarehouseId)
                                .thenComparing(DistributedOrderLineResponse::getProductId))
                        .toList())
                .build();
    }

    private Double toMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

}
