package com.example.csdlpt.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.example.csdlpt.dto.request.FallbackOrderRequest;
import com.example.csdlpt.dto.response.AvailableSiteResponse;
import com.example.csdlpt.dto.response.FallbackOrderAllocationResponse;
import com.example.csdlpt.dto.response.FallbackOrderResponse;
import com.example.csdlpt.entity.CustomerIdentity;
import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.entity.InventoryId;
import com.example.csdlpt.entity.Order;
import com.example.csdlpt.entity.OrderDetail;
import com.example.csdlpt.entity.OrderDetailId;
import com.example.csdlpt.entity.ProductBasic;
import com.example.csdlpt.entity.Site;
import com.example.csdlpt.entity.Warehouse;
import com.example.csdlpt.enums.OrderStatus;
import com.example.csdlpt.enums.SiteCode;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.site_dn.DanangInventoryRepository;
import com.example.csdlpt.repository.site_dn.DanangOrderDetailRepository;
import com.example.csdlpt.repository.site_dn.DanangOrderRepository;
import com.example.csdlpt.repository.site_hcm.HcmInventoryRepository;
import com.example.csdlpt.repository.site_hcm.HcmOrderDetailRepository;
import com.example.csdlpt.repository.site_hcm.HcmOrderRepository;
import com.example.csdlpt.repository.site_hn.HanoiInventoryRepository;
import com.example.csdlpt.repository.site_hn.HanoiOrderDetailRepository;
import com.example.csdlpt.repository.site_hn.HanoiOrderRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FallbackOrderService {

    InventoryService inventoryService;
    SiteRoutingService siteRoutingService;
    HanoiInventoryRepository hanoiInventoryRepository;
    DanangInventoryRepository danangInventoryRepository;
    HcmInventoryRepository hcmInventoryRepository;
    HanoiOrderRepository hanoiOrderRepository;
    DanangOrderRepository danangOrderRepository;
    HcmOrderRepository hcmOrderRepository;
    HanoiOrderDetailRepository hanoiOrderDetailRepository;
    DanangOrderDetailRepository danangOrderDetailRepository;
    HcmOrderDetailRepository hcmOrderDetailRepository;

    public FallbackOrderResponse createFallbackOrder(FallbackOrderRequest request) {
        validateRequest(request);
        SiteCode primarySite = findWarehouseSite(request.getPrimaryWarehouseId());
        ensureOrderIdAvailable(request.getOrderId());

        Inventory primaryInventory = findInventory(primarySite, request.getPrimaryWarehouseId(), request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.INSUFFICIENT_STOCK,
                        "Kho chính không có sản phẩm này"));

        int primaryAllocated = Math.min(primaryInventory.getQuantity(), request.getQuantity());
        int remaining = request.getQuantity() - primaryAllocated;
        List<Allocation> allocations = new ArrayList<>();

        if (primaryAllocated > 0) {
            allocations.add(new Allocation(primarySite, primaryInventory, primaryAllocated, false));
        }

        if (remaining > 0) {
            Allocation fallbackAllocation = findFallbackAllocation(request, primarySite, remaining);
            allocations.add(fallbackAllocation);
        }

        // GIỚI HẠN PHÂN TÁN (KNOWN LIMITATION):
        // Không có @Transactional bao phủ toàn bộ luồng vì các allocation có thể nằm ở
        // các datasource khác nhau (HN, DN, HCM). Spring @Transactional chỉ hoạt động
        // trong một datasource duy nhất; để đảm bảo tính nhất quán toàn cục cần 2PC
        // hoặc Saga pattern — ngoài phạm vi bài tập này.
        // Rủi ro thực tế: nếu persistAllocation thành công cho primary nhưng ném exception
        // khi xử lý fallback, inventory kho chính đã bị trừ và Order đã được lưu, trong
        // khi phần fallback chưa được tạo → dữ liệu không nhất quán giữa các site.
        allocations.forEach(allocation -> persistAllocation(request, allocation));

        List<FallbackOrderAllocationResponse> allocationResponses = allocations.stream()
                .map(this::toAllocationResponse)
                .toList();

        int fulfilledQuantity = allocationResponses.stream()
                .map(FallbackOrderAllocationResponse::getQuantity)
                .reduce(0, Integer::sum);

        log.info("Fallback order ID={} product={} requested={} fulfilled={} fallbackUsed={}",
                request.getOrderId(), request.getProductId(), request.getQuantity(), fulfilledQuantity, remaining > 0);

        return FallbackOrderResponse.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .productId(request.getProductId())
                .requestedQuantity(request.getQuantity())
                .fulfilledQuantity(fulfilledQuantity)
                .primarySiteCode(primarySite.name())
                .primaryWarehouseId(request.getPrimaryWarehouseId())
                .fallbackUsed(remaining > 0)
                .q2ReuseNote("Fallback gọi InventoryService.findSiteWithEnoughStock(productId, remainingQuantity), cùng logic với Q2 /api/inventories/available")
                .allocations(allocationResponses)
                .build();
    }

    private void validateRequest(FallbackOrderRequest request) {
        if (request.getOrderId() == null || request.getCustomerId() == null || request.getProductId() == null
                || request.getPrimaryWarehouseId() == null || request.getQuantity() == null) {
            throw new AppException(ErrorCode.INVALID_KEY, "Thiếu thông tin bắt buộc để xử lý fallback order");
        }
        if (request.getQuantity() <= 0) {
            throw new AppException(ErrorCode.INVALID_KEY, "Số lượng đặt hàng phải lớn hơn 0");
        }
    }

    private void ensureOrderIdAvailable(Long orderId) {
        if (hanoiOrderRepository.existsById(orderId)
                || danangOrderRepository.existsById(orderId)
                || hcmOrderRepository.existsById(orderId)) {
            throw new AppException(ErrorCode.INVALID_KEY, "Order ID đã tồn tại ở ít nhất một site");
        }
    }

    private Allocation findFallbackAllocation(FallbackOrderRequest request, SiteCode primarySite, int remaining) {
        List<AvailableSiteResponse> availableSites = inventoryService.findSiteWithEnoughStock(request.getProductId(), remaining);

        return availableSites.stream()
                .map(AvailableSiteResponse::getSiteCode)
                .map(SiteCode::valueOf)
                .sorted(Comparator.comparing(site -> site == primarySite))
                .map(site -> findWarehouseWithEnoughStock(site, request.getProductId(), remaining,
                        request.getPrimaryWarehouseId()))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.INSUFFICIENT_STOCK,
                        "Q2 có site đủ tổng tồn nhưng không tìm thấy warehouse đơn lẻ đủ phần thiếu"));
    }

    private Optional<Allocation> findWarehouseWithEnoughStock(
            SiteCode siteCode,
            Integer productId,
            Integer quantity,
            Integer excludedWarehouseId) {
        return findInventoriesByProduct(siteCode, productId).stream()
                .filter(inventory -> !inventory.getId().getWarehouseId().equals(excludedWarehouseId))
                .filter(inventory -> inventory.getQuantity() >= quantity)
                .sorted(Comparator.comparing(inventory -> inventory.getId().getWarehouseId()))
                .findFirst()
                .map(inventory -> new Allocation(siteCode, inventory, quantity, true));
    }

    private void persistAllocation(FallbackOrderRequest request, Allocation allocation) {
        Inventory inventory = allocation.inventory();
        inventory.setQuantity(inventory.getQuantity() - allocation.quantity());
        inventoryRepository(allocation.siteCode()).save(inventory);

        Warehouse warehouse = siteRoutingService.findWarehouseBySite(inventory.getId().getWarehouseId(), allocation.siteCode());
        ProductBasic product = siteRoutingService.findProductBySite(request.getProductId(), allocation.siteCode());
        BigDecimal price = request.getPrice() != null ? request.getPrice() : product.getPrice();

        Order order = Order.builder()
                .id(request.getOrderId())
                .customer(CustomerIdentity.builder().id(request.getCustomerId()).build())
                .status(OrderStatus.COMPLETED)
                .warehouse(warehouse)
                .site(Site.builder().id(siteId(allocation.siteCode())).build())
                .build();
        orderRepository(allocation.siteCode()).save(order);

        OrderDetail detail = OrderDetail.builder()
                .id(new OrderDetailId(request.getOrderId(), request.getProductId(), warehouse.getId()))
                .order(order)
                .product(product)
                .warehouse(warehouse)
                .quantity(allocation.quantity())
                .price(price)
                .build();
        orderDetailRepository(allocation.siteCode()).save(detail);
    }

    private Optional<Inventory> findInventory(SiteCode siteCode, Integer warehouseId, Integer productId) {
        InventoryId inventoryId = InventoryId.builder()
                .warehouseId(warehouseId)
                .productId(productId)
                .build();
        return inventoryRepository(siteCode).findById(inventoryId);
    }

    private List<Inventory> findInventoriesByProduct(SiteCode siteCode, Integer productId) {
        return switch (siteCode) {
            case DN -> danangInventoryRepository.findByProductId(productId);
            case HCM -> hcmInventoryRepository.findByProductId(productId);
            default -> hanoiInventoryRepository.findByProductId(productId);
        };
    }

    private SiteCode findWarehouseSite(Integer warehouseId) {
        for (SiteCode siteCode : SiteCode.values()) {
            try {
                siteRoutingService.findWarehouseBySite(warehouseId, siteCode);
                return siteCode;
            } catch (AppException ignored) {
                // Continue probing the next site because warehouse fragments are horizontal by site.
            }
        }
        throw new AppException(ErrorCode.WAREHOUSE_NOT_FOUND, "Không tìm thấy kho chính ở bất kỳ site nào");
    }

    private FallbackOrderAllocationResponse toAllocationResponse(Allocation allocation) {
        Inventory inventory = allocation.inventory();
        Warehouse warehouse = siteRoutingService.findWarehouseBySite(inventory.getId().getWarehouseId(), allocation.siteCode());
        return FallbackOrderAllocationResponse.builder()
                .siteCode(allocation.siteCode().name())
                .warehouseId(warehouse.getId())
                .warehouseCode(warehouse.getCode())
                .quantity(allocation.quantity())
                .remainingStock(inventory.getQuantity())
                .fallback(allocation.fallback())
                .build();
    }

    private JpaRepository<Inventory, InventoryId> inventoryRepository(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangInventoryRepository;
            case HCM -> hcmInventoryRepository;
            default -> hanoiInventoryRepository;
        };
    }

    private JpaRepository<Order, Long> orderRepository(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangOrderRepository;
            case HCM -> hcmOrderRepository;
            default -> hanoiOrderRepository;
        };
    }

    private JpaRepository<OrderDetail, OrderDetailId> orderDetailRepository(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangOrderDetailRepository;
            case HCM -> hcmOrderDetailRepository;
            default -> hanoiOrderDetailRepository;
        };
    }

    private Integer siteId(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> 2;
            case HCM -> 3;
            default -> 1;
        };
    }

    private record Allocation(SiteCode siteCode, Inventory inventory, Integer quantity, Boolean fallback) {
    }
}
