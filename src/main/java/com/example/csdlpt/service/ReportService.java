package com.example.csdlpt.service;

import com.example.csdlpt.dto.response.MonthlyRevenueResponse;
import com.example.csdlpt.dto.response.RevenueSummaryResponse;
import com.example.csdlpt.dto.response.TopSellingResponse;
import com.example.csdlpt.entity.ProductBasic;
import com.example.csdlpt.entity.Warehouse;
import com.example.csdlpt.enums.SiteCode;
import com.example.csdlpt.repository.common.DistributedOrderRepository;
import com.example.csdlpt.repository.projection.MonthlyRevenueProjection;
import com.example.csdlpt.repository.projection.TopSellingProjection;
import com.example.csdlpt.repository.site_dn.DanangOrderRepository;
import com.example.csdlpt.repository.site_hcm.HcmOrderRepository;
import com.example.csdlpt.repository.site_hn.HanoiOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final HanoiOrderRepository hanoiOrderRepository;
    private final DanangOrderRepository danangOrderRepository;
    private final HcmOrderRepository hcmOrderRepository;
    private final SiteRoutingService siteRoutingService;

    public RevenueSummaryResponse getMonthlyRevenue(Integer year) {
        List<MonthlyRevenueResponse> details = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (SiteCode site : SiteCode.values()) {
            for (MonthlyRevenueProjection row : orderRepo(site).getMonthlyRevenue(year)) {
                Warehouse warehouse = findWarehouseFromAnySite(row.getWarehouseId());
                BigDecimal revenue = row.getRevenue() == null ? BigDecimal.ZERO : row.getRevenue();
                details.add(MonthlyRevenueResponse.builder()
                        .year(year)
                        .month(row.getMonth())
                        .warehouseId(row.getWarehouseId())
                        .warehouseCode(warehouse.getCode())
                        .siteCode(site.name())
                        .revenue(revenue)
                        .build());
                total = total.add(revenue);
            }
        }

        details.sort(Comparator.comparing(MonthlyRevenueResponse::getMonth)
                .thenComparing(MonthlyRevenueResponse::getSiteCode));
        return RevenueSummaryResponse.builder()
                .year(year)
                .totalRevenue(total)
                .details(details)
                .build();
    }

    public List<TopSellingResponse> getTopSelling(Integer limit) {
        int finalLimit = limit == null || limit <= 0 ? 5 : limit;
        Map<Integer, Long> totalByProduct = new HashMap<>();

        for (SiteCode site : SiteCode.values()) {
            for (TopSellingProjection row : orderRepo(site).getTopSellingLocal()) {
                totalByProduct.merge(row.getProductId(), row.getTotalSold(), Long::sum);
            }
        }

        return totalByProduct.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(finalLimit)
                .map(entry -> {
                    ProductBasic product = siteRoutingService.findProductBySite(entry.getKey(), SiteCode.HN);
                    return TopSellingResponse.builder()
                            .productId(entry.getKey())
                            .productName(product.getName())
                            .totalSold(entry.getValue())
                            .build();
                })
                .toList();
    }

    private Warehouse findWarehouseFromAnySite(Integer warehouseId) {
        for (SiteCode site : SiteCode.values()) {
            try {
                return siteRoutingService.findWarehouseBySite(warehouseId, site);
            } catch (RuntimeException ignored) {
                // Theo schema main, warehouse có thể chỉ nằm ở site cục bộ của nó.
            }
        }
        throw new com.example.csdlpt.exception.AppException(
                com.example.csdlpt.exception.ErrorCode.WAREHOUSE_NOT_FOUND,
                "Không tìm thấy kho id=" + warehouseId);
    }

    private DistributedOrderRepository orderRepo(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangOrderRepository;
            case HCM -> hcmOrderRepository;
            default -> hanoiOrderRepository;
        };
    }
}
