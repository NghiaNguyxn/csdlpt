package com.example.csdlpt.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.csdlpt.context.SiteContextHolder;
import com.example.csdlpt.dto.request.InventoryRequest;
import com.example.csdlpt.dto.response.AvailableSiteResponse;
import com.example.csdlpt.dto.response.InventoryResponse;
import com.example.csdlpt.dto.response.StockResponse;
import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.entity.InventoryId;
import com.example.csdlpt.entity.ProductBasic;
import com.example.csdlpt.entity.Warehouse;
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
public class InventoryService {

    SiteRoutingService siteRoutingService;
    InventoryTransactionService inventoryTransactionService;

    public StockResponse getStockBySite(Integer productId, SiteCode siteCode) {
        if (siteCode == null) {
            siteCode = SiteContextHolder.getCurrentSite();
            log.info("Thực thi getInventoryBySite tại Local Site: {}", siteCode);
        }

        List<Warehouse> warehouses = siteRoutingService.findAllWareHouseBySite(siteCode);
        ProductBasic product = siteRoutingService.findProductBySite(productId, siteCode);
        var inventoryRepo = siteRoutingService.getInventoryRepository(siteCode);

        Integer totalQuantity = warehouses.stream()
                .map(warehouse -> {
                    InventoryId inventoryId = InventoryId.builder()
                            .warehouseId(warehouse.getId())
                            .productId(productId)
                            .build();

                    Inventory inventory = inventoryRepo.findById(inventoryId)
                            .orElseGet(() -> Inventory.builder()
                                    .id(inventoryId)
                                    .product(product)
                                    .warehouse(warehouse)
                                    .quantity(0)
                                    .build());

                    return inventory.getQuantity();
                })
                .reduce(0, Integer::sum, Integer::sum);

        return StockResponse.builder()
                .productId(productId)
                .quantity(totalQuantity)
                .siteCode(siteCode.name())
                .build();
    }

    public StockResponse getGlobalStock(Integer productId) {
        SiteCode localSiteCode = SiteContextHolder.getCurrentSite();
        log.info("Thực thi getGlobalStock tại Local Site: {}", localSiteCode);

        int totalQuantity = 0;
        for (SiteCode site : SiteCode.values()) {
            totalQuantity += getSafeStock(productId, site);
        }

        return StockResponse.builder()
                .productId(productId)
                .quantity(totalQuantity)
                .siteCode("GLOBAL")
                .build();
    }

    public List<AvailableSiteResponse> findSiteWithEnoughStock(Integer productId, Integer quantity) {
        List<AvailableSiteResponse> availableSite = new ArrayList<>();

        for (SiteCode site : SiteCode.values()) {
            int stock = getSafeStock(productId, site);
            if (stock >= quantity) {
                availableSite.add(AvailableSiteResponse.builder()
                        .siteCode(site.name())
                        .siteName(site.getSiteName())
                        .productId(productId)
                        .quantity(stock)
                        .build());
            }
        }

        if (availableSite.isEmpty()) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        return availableSite;
    }

    public InventoryResponse addStock(InventoryRequest request) {
        SiteCode localSiteCode = SiteContextHolder.getCurrentSite();
        SiteCode targetSiteCode = resolveTargetSite(request);
        log.info("Thực thi addStock từ Local Site: {} tới Target Site: {}", localSiteCode, targetSiteCode);

        ProductBasic product = siteRoutingService.findProductBySite(request.getProductId(), targetSiteCode);
        Warehouse warehouse = siteRoutingService.findWarehouseBySite(request.getWarehouseId(), targetSiteCode);

        InventoryId inventoryId = InventoryId.builder()
                .warehouseId(warehouse.getId())
                .productId(product.getId())
                .build();

        return switch (targetSiteCode) {
            case DN -> inventoryTransactionService.addStockAtDanang(
                    inventoryId, product, warehouse, request.getQuantity());
            case HCM -> inventoryTransactionService.addStockAtHcm(
                    inventoryId, product, warehouse, request.getQuantity());
            default -> inventoryTransactionService.addStockAtHanoi(
                    inventoryId, product, warehouse, request.getQuantity());
        };
    }

    public InventoryResponse reduceStock(InventoryRequest request) {
        SiteCode localSiteCode = SiteContextHolder.getCurrentSite();
        SiteCode targetSiteCode = resolveTargetSite(request);
        log.info("Thực thi reduceStock từ Local Site: {} tới Target Site: {}", localSiteCode, targetSiteCode);

        Warehouse warehouse = siteRoutingService.findWarehouseBySite(request.getWarehouseId(), targetSiteCode);
        ProductBasic product = siteRoutingService.findProductBySite(request.getProductId(), targetSiteCode);

        InventoryId inventoryId = InventoryId.builder()
                .warehouseId(warehouse.getId())
                .productId(product.getId())
                .build();

        return switch (targetSiteCode) {
            case DN -> inventoryTransactionService.reduceStockAtDanang(
                    inventoryId, request.getQuantity());
            case HCM -> inventoryTransactionService.reduceStockAtHcm(
                    inventoryId, request.getQuantity());
            default -> inventoryTransactionService.reduceStockAtHanoi(
                    inventoryId, request.getQuantity());
        };
    }

    private SiteCode resolveTargetSite(InventoryRequest request) {
        if (request.getProductId() == null || request.getWarehouseId() == null
                || request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new AppException(ErrorCode.INVALID_KEY, "Thông tin tồn kho không hợp lệ");
        }

        return request.getTargetSite() != null
                ? request.getTargetSite()
                : SiteContextHolder.getCurrentSite();
    }

    // Tránh delay do lazy replication.
    private Integer getSafeStock(Integer productId, SiteCode siteCode) {
        try {
            return getStockBySite(productId, siteCode).getQuantity();
        } catch (AppException e) {
            return 0;
        }
    }
}
