package com.example.csdlpt.service.Order;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.csdlpt.dto.request.LocalOrderItemRequest;
import com.example.csdlpt.dto.request.LocalOrderRequest;
import com.example.csdlpt.dto.response.StockResponse;
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
import com.example.csdlpt.repository.InventoryLockingRepository;
import com.example.csdlpt.repository.site_dn.DanangCustomerIdentityRepository;
import com.example.csdlpt.repository.site_dn.DanangInventoryRepository;
import com.example.csdlpt.repository.site_dn.DanangProductRepository;
import com.example.csdlpt.repository.site_dn.DanangWarehouseRepository;
import com.example.csdlpt.repository.site_hcm.HcmCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hcm.HcmInventoryRepository;
import com.example.csdlpt.repository.site_hcm.HcmProductRepository;
import com.example.csdlpt.repository.site_hcm.HcmWarehouseRepository;
import com.example.csdlpt.repository.site_hn.HanoiCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hn.HanoiInventoryRepository;
import com.example.csdlpt.repository.site_hn.HanoiProductRepository;
import com.example.csdlpt.repository.site_hn.HanoiWarehouseRepository;
import com.example.csdlpt.service.InventoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocalOrderTransactionService {
    private final OrderPersistenceService orderPersistenceService;
    private final InventoryService inventoryService;
    private final HanoiCustomerIdentityRepository hanoiCustomerIdentityRepository;
    private final HanoiProductRepository hanoiProductRepository;
    private final HanoiWarehouseRepository hanoiWarehouseRepository;
    private final HanoiInventoryRepository hanoiInventoryRepository;

    private final DanangCustomerIdentityRepository danangCustomerIdentityRepository;
    private final DanangProductRepository danangProductRepository;
    private final DanangWarehouseRepository danangWarehouseRepository;
    private final DanangInventoryRepository danangInventoryRepository;

    private final HcmCustomerIdentityRepository hcmCustomerIdentityRepository;
    private final HcmProductRepository hcmProductRepository;
    private final HcmWarehouseRepository hcmWarehouseRepository;
    private final HcmInventoryRepository hcmInventoryRepository;

    @Transactional("hanoiTransactionManager")
    public Order createLocalOrderAtHanoi(LocalOrderRequest request) {
        return createLocalOrder(request, hanoiCustomerIdentityRepository, hanoiProductRepository,
                hanoiWarehouseRepository, hanoiInventoryRepository, 1, "HN", SiteCode.HN);
    }

    @Transactional("danangTransactionManager")
    public Order createLocalOrderAtDanang(LocalOrderRequest request) {
        return createLocalOrder(request, danangCustomerIdentityRepository, danangProductRepository,
                danangWarehouseRepository, danangInventoryRepository, 2, "DN", SiteCode.DN);
    }

    @Transactional("hcmTransactionManager")
    public Order createLocalOrderAtHcm(LocalOrderRequest request) {
        return createLocalOrder(request, hcmCustomerIdentityRepository, hcmProductRepository,
                hcmWarehouseRepository, hcmInventoryRepository, 3, "HCM", SiteCode.HCM);
    }

    @Transactional("hanoiTransactionManager")
    public Order updateStatusAtHanoi(Long orderId, OrderStatus status) {
        return orderPersistenceService.updateOrderStatusAndReturnAtHanoi(orderId, status);
    }

    @Transactional("danangTransactionManager")
    public Order updateStatusAtDanang(Long orderId, OrderStatus status) {
        return orderPersistenceService.updateOrderStatusAndReturnAtDanang(orderId, status);
    }

    @Transactional("hcmTransactionManager")
    public Order updateStatusAtHcm(Long orderId, OrderStatus status) {
        return orderPersistenceService.updateOrderStatusAndReturnAtHcm(orderId, status);
    }

    private <C extends org.springframework.data.jpa.repository.JpaRepository<CustomerIdentity, Long>,
            P extends org.springframework.data.jpa.repository.JpaRepository<ProductBasic, Integer>,
            W extends org.springframework.data.jpa.repository.JpaRepository<Warehouse, Integer>,
            I extends InventoryLockingRepository>
    Order createLocalOrder(LocalOrderRequest request, C customerRepo, P productRepo, W warehouseRepo,
                           I inventoryRepo, Integer siteId, String siteCode, SiteCode siteCodeEnum) {
        List<LocalProductDemand> demands = normalizeItems(request);
        verifyGlobalStockForLocalOrder(demands);

        CustomerIdentity customer = customerRepo.findById(request.getCustomerId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));

        Long orderId = generateOrderId();
        Order order = Order.builder()
                .id(orderId)
                .customer(customer)
                .status(OrderStatus.PENDING)
                .site(siteRef(siteId, siteCode))
                .build();

        List<OrderDetail> details = new ArrayList<>();
        for (LocalProductDemand demand : demands) {
            ProductBasic product = productRepo.findById(demand.productId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            List<LocalInventoryAllocation> allocations = allocateFromLocalWarehouses(
                    demand, warehouseRepo, inventoryRepo, siteId, siteCode);

            for (LocalInventoryAllocation allocation : allocations) {
                details.add(OrderDetail.builder()
                        .id(OrderDetailId.builder()
                                .orderId(orderId)
                                .productId(demand.productId())
                                .warehouseId(allocation.warehouseId())
                                .build())
                        .order(order)
                        .product(product)
                        .quantity(allocation.quantity())
                        .price(product.getPrice())
                        .build());
            }
        }

        return switch (siteCodeEnum) {
            case DN -> orderPersistenceService.createOrderAtDanang(order, details);
            case HCM -> orderPersistenceService.createOrderAtHcm(order, details);
            default -> orderPersistenceService.createOrderAtHanoi(order, details);
        };
    }

    private List<LocalProductDemand> normalizeItems(LocalOrderRequest request) {
        validateRequest(request);
        Map<Integer, Integer> quantityByProduct = new LinkedHashMap<>();

        for (LocalOrderItemRequest item : request.getItems()) {
            quantityByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        List<LocalProductDemand> normalizedItems = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : quantityByProduct.entrySet()) {
            normalizedItems.add(new LocalProductDemand(
                    entry.getKey(),
                    entry.getValue()));
        }
        normalizedItems.sort(Comparator.comparing(LocalProductDemand::productId));
        return normalizedItems;
    }

    private void verifyGlobalStockForLocalOrder(List<LocalProductDemand> demands) {
        for (LocalProductDemand demand : demands) {
            StockResponse globalStock = inventoryService.getGlobalStock(demand.productId());
            int availableQuantity = globalStock.getQuantity() == null ? 0 : globalStock.getQuantity();
            if (availableQuantity < demand.quantity()) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                        "Ton kho toan he thong khong du cho productId=" + demand.productId()
                                + ", so luong can=" + demand.quantity()
                                + ", so luong kha dung=" + availableQuantity);
            }
        }
    }

    private <W extends org.springframework.data.jpa.repository.JpaRepository<Warehouse, Integer>,
            I extends InventoryLockingRepository>
    List<LocalInventoryAllocation> allocateFromLocalWarehouses(
            LocalProductDemand demand,
            W warehouseRepo,
            I inventoryRepo,
            Integer siteId,
            String siteCode) {
        List<Warehouse> localWarehouses = warehouseRepo.findAll().stream()
                .filter(warehouse -> belongsToSite(warehouse, siteId))
                .sorted(Comparator.comparing(Warehouse::getId))
                .toList();

        if (localWarehouses.isEmpty()) {
            throw new AppException(ErrorCode.WAREHOUSE_NOT_FOUND,
                    "Khong tim thay kho nao thuoc site local " + siteCode);
        }

        List<LockedLocalInventory> lockedInventories = new ArrayList<>();
        int availableLocalQuantity = 0;
        for (Warehouse warehouse : localWarehouses) {
            InventoryId inventoryId = InventoryId.builder()
                    .warehouseId(warehouse.getId())
                    .productId(demand.productId())
                    .build();
            Inventory inventory = inventoryRepo.findByIdForUpdate(inventoryId).orElse(null);
            if (inventory == null || inventory.getQuantity() == null || inventory.getQuantity() <= 0) {
                continue;
            }

            int currentQuantity = inventory.getQuantity();
            lockedInventories.add(new LockedLocalInventory(warehouse.getId(), inventory, currentQuantity));
            availableLocalQuantity += currentQuantity;
        }

        if (availableLocalQuantity < demand.quantity()) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                    "Kho local " + siteCode + " khong du hang cho productId=" + demand.productId()
                            + ", so luong can=" + demand.quantity()
                            + ", so luong kha dung tai site=" + availableLocalQuantity);
        }

        List<LockedLocalInventory> allocationOrder = lockedInventories.stream()
                .sorted(Comparator.comparingInt(LockedLocalInventory::availableQuantity).reversed()
                        .thenComparingInt(LockedLocalInventory::warehouseId))
                .toList();
        int remaining = demand.quantity();
        List<LocalInventoryAllocation> allocations = new ArrayList<>();

        for (LockedLocalInventory lockedInventory : allocationOrder) {
            if (remaining == 0) {
                break;
            }

            Inventory inventory = lockedInventory.inventory();
            int allocatedQuantity = Math.min(remaining, inventory.getQuantity());
            inventory.setQuantity(inventory.getQuantity() - allocatedQuantity);
            inventoryRepo.save(inventory);

            allocations.add(new LocalInventoryAllocation(lockedInventory.warehouseId(), allocatedQuantity));
            remaining -= allocatedQuantity;
        }

        if (remaining > 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                    "Kho local " + siteCode + " khong du hang cho productId=" + demand.productId());
        }
        return allocations;
    }

    private void validateRequest(LocalOrderRequest request) {
        if (request == null || request.getCustomerId() == null) {
            throw new AppException(ErrorCode.INVALID_KEY, "Thieu customerId");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY, "Don hang phai co it nhat mot san pham");
        }
        for (LocalOrderItemRequest item : request.getItems()) {
            if (item.getProductId() == null
                    || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new AppException(ErrorCode.INVALID_KEY, "Thong tin item khong hop le");
            }
        }
    }

    private boolean belongsToSite(Warehouse warehouse, Integer siteId) {
        return warehouse != null
                && warehouse.getSite() != null
                && warehouse.getSite().getId() != null
                && warehouse.getSite().getId().equals(siteId);
    }

    private Site siteRef(Integer siteId, String siteCode) {
        return Site.builder().id(siteId).siteCode(siteCode).build();
    }

    private Long generateOrderId() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }

    private record LocalProductDemand(Integer productId, Integer quantity) {
    }

    private record LocalInventoryAllocation(Integer warehouseId, Integer quantity) {
    }

    private record LockedLocalInventory(Integer warehouseId, Inventory inventory, int availableQuantity) {
    }
}
