package com.example.csdlpt.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.csdlpt.dto.response.MonthlyRevenueResponse;
import com.example.csdlpt.dto.response.RevenueSummaryResponse;
import com.example.csdlpt.dto.response.SiteRevenueResponse;
import com.example.csdlpt.dto.response.TopSellingResponse;
import com.example.csdlpt.dto.response.TotalRevenueResponse;
import com.example.csdlpt.dto.response.WarehouseRevenueResponse;
import com.example.csdlpt.entity.Order;
import com.example.csdlpt.entity.OrderDetail;
import com.example.csdlpt.entity.Warehouse;
import com.example.csdlpt.enums.OrderStatus;
import com.example.csdlpt.enums.SiteCode;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.site_dn.DanangOrderDetailRepository;
import com.example.csdlpt.repository.site_dn.DanangOrderRepository;
import com.example.csdlpt.repository.site_dn.DanangProductRepository;
import com.example.csdlpt.repository.site_dn.DanangWarehouseRepository;
import com.example.csdlpt.repository.site_hcm.HcmOrderDetailRepository;
import com.example.csdlpt.repository.site_hcm.HcmOrderRepository;
import com.example.csdlpt.repository.site_hcm.HcmProductRepository;
import com.example.csdlpt.repository.site_hcm.HcmWarehouseRepository;
import com.example.csdlpt.repository.site_hn.HanoiOrderDetailRepository;
import com.example.csdlpt.repository.site_hn.HanoiOrderRepository;
import com.example.csdlpt.repository.site_hn.HanoiProductRepository;
import com.example.csdlpt.repository.site_hn.HanoiWarehouseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final HanoiOrderRepository hanoiOrderRepository;
    private final HanoiOrderDetailRepository hanoiOrderDetailRepository;
    private final HanoiProductRepository hanoiProductRepository;
    private final HanoiWarehouseRepository hanoiWarehouseRepository;

    private final DanangOrderRepository danangOrderRepository;
    private final DanangOrderDetailRepository danangOrderDetailRepository;
    private final DanangProductRepository danangProductRepository;
    private final DanangWarehouseRepository danangWarehouseRepository;

    private final HcmOrderRepository hcmOrderRepository;
    private final HcmOrderDetailRepository hcmOrderDetailRepository;
    private final HcmProductRepository hcmProductRepository;
    private final HcmWarehouseRepository hcmWarehouseRepository;

    public RevenueSummaryResponse getMonthlyRevenue(Integer year) {
        validateYear(year);
        List<MonthlyRevenueResponse> details = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            int finalMonth = month;
            for (SiteCode siteCode : SiteCode.values()) {
                BigDecimal revenue = orders(siteCode).stream()
                        .filter(order -> belongsToYearMonth(order, year, finalMonth))
                        .filter(this::isRevenueOrder)
                        .map(order -> orderRevenue(order.getId(), siteCode))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                details.add(MonthlyRevenueResponse.builder()
                        .year(year)
                        .month(month)
                        .siteCode(siteCode.name())
                        .revenue(revenue)
                        .build());
            }
        }
        List<SiteRevenueResponse> siteBreakdown = getRevenueBySite(year);
        BigDecimal total = siteBreakdown.stream().map(SiteRevenueResponse::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return RevenueSummaryResponse.builder()
                .year(year)
                .totalRevenue(total)
                .details(details)
                .siteBreakdown(siteBreakdown)
                .build();
    }

    public List<SiteRevenueResponse> getRevenueBySite(Integer year) {
        validateYear(year);
        List<SiteRevenueResponse> result = new ArrayList<>();
        for (SiteCode siteCode : SiteCode.values()) {
            BigDecimal revenue = orders(siteCode).stream()
                    .filter(order -> order.getOrderDate() != null && order.getOrderDate().getYear() == year)
                    .filter(this::isRevenueOrder)
                    .map(order -> orderRevenue(order.getId(), siteCode))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(SiteRevenueResponse.builder()
                    .year(year)
                    .siteCode(siteCode.name())
                    .revenue(revenue)
                    .build());
        }
        return result;
    }

    public List<WarehouseRevenueResponse> getRevenueByWarehouse(Integer year) {
        validateYear(year);
        Map<Integer, Warehouse> warehouseById = warehouseMetadataById();
        Map<WarehouseRevenueKey, BigDecimal> revenueByWarehouse = new HashMap<>();

        for (SiteCode sourceSite : SiteCode.values()) {
            Set<Long> revenueOrderIds = new HashSet<>();
            orders(sourceSite).stream()
                    .filter(order -> order.getOrderDate() != null && order.getOrderDate().getYear() == year)
                    .filter(this::isRevenueOrder)
                    .map(Order::getId)
                    .forEach(revenueOrderIds::add);

            details(sourceSite).stream()
                    .filter(detail -> detail.getId() != null
                            && revenueOrderIds.contains(detail.getId().getOrderId()))
                    .forEach(detail -> {
                        Integer warehouseId = detail.getId().getWarehouseId();
                        Warehouse warehouse = warehouseById.get(warehouseId);
                        WarehouseRevenueKey key = toWarehouseRevenueKey(year, sourceSite, warehouseId, warehouse);
                        BigDecimal lineRevenue = safe(detail.getPrice())
                                .multiply(BigDecimal.valueOf(detail.getQuantity() == null ? 0 : detail.getQuantity()));
                        revenueByWarehouse.merge(key, lineRevenue, BigDecimal::add);
                    });
        }

        return revenueByWarehouse.entrySet().stream()
                .sorted(Comparator
                        .comparing((Map.Entry<WarehouseRevenueKey, BigDecimal> entry) -> entry.getKey().siteCode())
                        .thenComparing(entry -> entry.getKey().warehouseId()))
                .map(entry -> WarehouseRevenueResponse.builder()
                        .year(entry.getKey().year())
                        .siteCode(entry.getKey().siteCode())
                        .region(entry.getKey().region())
                        .warehouseId(entry.getKey().warehouseId())
                        .warehouseCode(entry.getKey().warehouseCode())
                        .warehouseName(entry.getKey().warehouseName())
                        .revenue(entry.getValue())
                        .build())
                .toList();
    }

    public TotalRevenueResponse getTotalRevenue(Integer year) {
        List<SiteRevenueResponse> siteBreakdown = getRevenueBySite(year);
        BigDecimal total = siteBreakdown.stream()
                .map(SiteRevenueResponse::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return TotalRevenueResponse.builder()
                .year(year)
                .totalRevenue(total)
                .siteBreakdown(siteBreakdown)
                .build();
    }

    public List<TopSellingResponse> getTopSelling(Integer limit) {
        int finalLimit = limit == null || limit <= 0 ? 5 : limit;
        Map<Integer, Long> quantityByProduct = new HashMap<>();
        for (SiteCode siteCode : SiteCode.values()) {
            Set<Long> revenueOrderIds = revenueOrderIds(siteCode);
            details(siteCode).stream()
                    .filter(detail -> detail.getId() != null && revenueOrderIds.contains(detail.getId().getOrderId()))
                    .forEach(detail -> quantityByProduct.merge(detail.getId().getProductId(),
                            detail.getQuantity() == null ? 0L : detail.getQuantity().longValue(), Long::sum));
        }
        return quantityByProduct.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(finalLimit)
                .map(entry -> TopSellingResponse.builder()
                        .productId(entry.getKey())
                        .productName(findProductName(entry.getKey()))
                        .totalSold(entry.getValue())
                        .build())
                .toList();
    }

    private boolean belongsToYearMonth(Order order, Integer year, Integer month) {
        return order.getOrderDate() != null
                && order.getOrderDate().getYear() == year
                && order.getOrderDate().getMonthValue() == month;
    }

    private boolean isRevenueOrder(Order order) {
        return order != null && order.getStatus() == OrderStatus.COMPLETED;
    }

    private BigDecimal orderRevenue(Long orderId, SiteCode siteCode) {
        return details(siteCode).stream()
                .filter(detail -> detail.getId() != null && orderId.equals(detail.getId().getOrderId()))
                .map(detail -> safe(detail.getPrice()).multiply(BigDecimal.valueOf(detail.getQuantity() == null ? 0 : detail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Order> orders(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangOrderRepository.findAll();
            case HCM -> hcmOrderRepository.findAll();
            default -> hanoiOrderRepository.findAll();
        };
    }

    private List<OrderDetail> details(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangOrderDetailRepository.findAll();
            case HCM -> hcmOrderDetailRepository.findAll();
            default -> hanoiOrderDetailRepository.findAll();
        };
    }

    private Map<Integer, Warehouse> warehouseMetadataById() {
        Map<Integer, Warehouse> warehouseById = new HashMap<>();
        hanoiWarehouseRepository.findAll().forEach(warehouse -> warehouseById.putIfAbsent(warehouse.getId(), warehouse));
        danangWarehouseRepository.findAll().forEach(warehouse -> warehouseById.putIfAbsent(warehouse.getId(), warehouse));
        hcmWarehouseRepository.findAll().forEach(warehouse -> warehouseById.putIfAbsent(warehouse.getId(), warehouse));
        return warehouseById;
    }

    private WarehouseRevenueKey toWarehouseRevenueKey(
            Integer year,
            SiteCode sourceSite,
            Integer warehouseId,
            Warehouse warehouse) {
        if (warehouse == null) {
            return new WarehouseRevenueKey(year, sourceSite.name(), null, warehouseId,
                    "warehouseId=" + warehouseId, "warehouseId=" + warehouseId);
        }
        String warehouseSiteCode = warehouse.getSite() == null || warehouse.getSite().getSiteCode() == null
                ? sourceSite.name()
                : warehouse.getSite().getSiteCode();
        return new WarehouseRevenueKey(year, warehouseSiteCode, warehouse.getRegion(), warehouseId,
                warehouse.getCode(), warehouse.getName());
    }

    private Set<Long> revenueOrderIds(SiteCode siteCode) {
        Set<Long> ids = new HashSet<>();
        orders(siteCode).stream()
                .filter(this::isRevenueOrder)
                .map(Order::getId)
                .forEach(ids::add);
        return ids;
    }

    private String findProductName(Integer productId) {
        return hanoiProductRepository.findById(productId)
                .or(() -> danangProductRepository.findById(productId))
                .or(() -> hcmProductRepository.findById(productId))
                .map(product -> product.getName())
                .orElse("productId=" + productId);
    }

    private void validateYear(Integer year) {
        int currentYear = LocalDate.now().getYear();
        if (year == null || year < 2000 || year > currentYear + 1) {
            throw new AppException(ErrorCode.INVALID_KEY, "Năm báo cáo không hợp lệ: " + year);
        }
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record WarehouseRevenueKey(
            Integer year,
            String siteCode,
            String region,
            Integer warehouseId,
            String warehouseCode,
            String warehouseName) {
    }
}
