package com.example.csdlpt.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.example.csdlpt.dto.response.DistributedOrderLineResponse;
import com.example.csdlpt.dto.response.MultiWarehouseOrderResponse;
import com.example.csdlpt.repository.MultiWarehouseOrderLineProjection;
import com.example.csdlpt.repository.site_dn.DanangOrderDetailRepository;
import com.example.csdlpt.repository.site_hcm.HcmOrderDetailRepository;
import com.example.csdlpt.repository.site_hn.HanoiOrderDetailRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Q5 — Distributed Join: tìm đơn hàng có sản phẩm xuất từ nhiều kho/site.
 *
 * Kiến trúc dữ liệu: mỗi order (kèm toàn bộ order_detail) được lưu tại site
 * coordinator tạo đơn. Warehouse và Site được replicate ở tất cả các site.
 *
 * Chiến lược pushdown:
 *   - Mỗi site chạy CTE để GROUP BY / HAVING lọc qualifying order_id tại DB.
 *   - Chỉ kéo về các dòng detail của đơn đủ điều kiện (không kéo toàn bộ).
 *   - Coordinator (Java) union kết quả từ 3 site, format response.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DistributedQueryService {

    HanoiOrderDetailRepository hanoiOrderDetailRepository;
    DanangOrderDetailRepository danangOrderDetailRepository;
    HcmOrderDetailRepository hcmOrderDetailRepository;

    // Q5-A: nhiều kho: Mỗi site chạy CTE lọc order có COUNT(DISTINCT warehouse_id) > 1.
    // Union kết quả từ HN + DN + HCM (mỗi order_id chỉ tồn tại ở 1 site).
    public List<MultiWarehouseOrderResponse> findOrdersFromMultipleWarehouses() {
        log.info("[Q5-A] Bắt đầu distributed join — đơn hàng từ nhiều kho");

        List<DistributedOrderLineResponse> allLines = Stream.of(
                hanoiOrderDetailRepository.findMultiWarehouseOrderLines(),
                danangOrderDetailRepository.findMultiWarehouseOrderLines(),
                hcmOrderDetailRepository.findMultiWarehouseOrderLines())
                .flatMap(List::stream)
                .map(this::toLineResponse)
                .toList();

        return buildResponse(allLines);
    }

    // Q5-B: nhiều site: Mỗi site chạy CTE lọc order có COUNT(DISTINCT w.site_id) > 1
    // Union kết quả từ HN + DN + HCM.
    public List<MultiWarehouseOrderResponse> findOrdersFromMultipleSites() {
        log.info("[Q5-B] Bắt đầu distributed join — đơn hàng từ nhiều site");

        List<DistributedOrderLineResponse> allLines = Stream.of(
                hanoiOrderDetailRepository.findMultiSiteOrderLines(),
                danangOrderDetailRepository.findMultiSiteOrderLines(),
                hcmOrderDetailRepository.findMultiSiteOrderLines())
                .flatMap(List::stream)
                .map(this::toLineResponse)
                .toList();

        return buildResponse(allLines);
    }

    // Group các dòng detail theo order_id (mỗi order_id chỉ ở 1 site nên không
    // có trùng lặp sau union), tính warehouseCount và siteCount, format response.
    private List<MultiWarehouseOrderResponse> buildResponse(List<DistributedOrderLineResponse> lines) {
        Map<Long, List<DistributedOrderLineResponse>> byOrder = lines.stream()
                .collect(Collectors.groupingBy(DistributedOrderLineResponse::getOrderId));

        return byOrder.entrySet().stream()
                .map(e -> toMultiWarehouseOrder(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(MultiWarehouseOrderResponse::getOrderId))
                .toList();
    }

    private DistributedOrderLineResponse toLineResponse(MultiWarehouseOrderLineProjection p) {
        return DistributedOrderLineResponse.builder()
                .orderId(p.getOrderId())
                .siteCode(p.getSiteCode())
                .warehouseId(p.getWarehouseId())
                .warehouseCode(p.getWarehouseCode())
                .warehouseName(p.getWarehouseName())
                .productId(p.getProductId())
                .productName(p.getProductName())
                .quantity(p.getQuantity())
                .price(p.getPrice())
                .build();
    }

    private MultiWarehouseOrderResponse toMultiWarehouseOrder(Long orderId, List<DistributedOrderLineResponse> lines) {
        List<String> siteCodes = lines.stream()
                .map(DistributedOrderLineResponse::getSiteCode)
                .distinct().sorted().toList();

        List<Integer> warehouseIds = lines.stream()
                .map(DistributedOrderLineResponse::getWarehouseId)
                .distinct().toList();

        List<String> warehouseCodes = lines.stream()
                .map(DistributedOrderLineResponse::getWarehouseCode)
                .distinct().sorted().toList();

        return MultiWarehouseOrderResponse.builder()
                .orderId(orderId)
                .warehouseCount(warehouseIds.size())
                .siteCount(siteCodes.size())
                .siteCodes(siteCodes)
                .warehouseCodes(warehouseCodes)
                .lines(lines.stream()
                        .sorted(Comparator.comparing(DistributedOrderLineResponse::getSiteCode)
                                .thenComparing(DistributedOrderLineResponse::getWarehouseId)
                                .thenComparing(DistributedOrderLineResponse::getProductId))
                        .toList())
                .build();
    }
}
