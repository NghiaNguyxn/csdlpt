package com.example.csdlpt.service;

import java.util.ArrayList;
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
import com.example.csdlpt.repository.site_dn.DanangOrderDetailRepository;
import com.example.csdlpt.repository.site_dn.DanangOrderRepository;
import com.example.csdlpt.repository.site_dn.DanangProductRepository;
import com.example.csdlpt.repository.site_dn.DanangWarehouseRepository;
import com.example.csdlpt.repository.site_hcm.HcmCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hcm.HcmInventoryRepository;
import com.example.csdlpt.repository.site_hcm.HcmOrderDetailRepository;
import com.example.csdlpt.repository.site_hcm.HcmOrderRepository;
import com.example.csdlpt.repository.site_hcm.HcmProductRepository;
import com.example.csdlpt.repository.site_hcm.HcmWarehouseRepository;
import com.example.csdlpt.repository.site_hn.HanoiCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hn.HanoiInventoryRepository;
import com.example.csdlpt.repository.site_hn.HanoiOrderDetailRepository;
import com.example.csdlpt.repository.site_hn.HanoiOrderRepository;
import com.example.csdlpt.repository.site_hn.HanoiProductRepository;
import com.example.csdlpt.repository.site_hn.HanoiWarehouseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocalOrderTransactionService {
    private final OrderPersistenceService orderPersistenceService;
    private final InventoryService inventoryService;
    private final HanoiOrderRepository hanoiOrderRepository;
    private final HanoiOrderDetailRepository hanoiOrderDetailRepository;
    private final HanoiCustomerIdentityRepository hanoiCustomerIdentityRepository;
    private final HanoiProductRepository hanoiProductRepository;
    private final HanoiWarehouseRepository hanoiWarehouseRepository;
    private final HanoiInventoryRepository hanoiInventoryRepository;

    private final DanangOrderRepository danangOrderRepository;
    private final DanangOrderDetailRepository danangOrderDetailRepository;
    private final DanangCustomerIdentityRepository danangCustomerIdentityRepository;
    private final DanangProductRepository danangProductRepository;
    private final DanangWarehouseRepository danangWarehouseRepository;
    private final DanangInventoryRepository danangInventoryRepository;

    private final HcmOrderRepository hcmOrderRepository;
    private final HcmOrderDetailRepository hcmOrderDetailRepository;
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
        List<LocalOrderLine> items = normalizeItems(request);
        verifyGlobalStockForLocalOrder(items);

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
        for (LocalOrderLine item : items) {
            ProductBasic product = productRepo.findById(item.productId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
            Warehouse warehouse = warehouseRepo.findById(item.warehouseId())
                    .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
            validateWarehouseBelongsToSite(warehouse, siteId, siteCode);

            InventoryId inventoryId = InventoryId.builder()
                    .warehouseId(item.warehouseId())
                    .productId(item.productId())
                    .build();
            Inventory inventory = inventoryRepo.findByIdForUpdate(inventoryId)
                    .orElseThrow(() -> new AppException(ErrorCode.INSUFFICIENT_STOCK,
                            "Khong co ton kho cho productId=" + item.productId()
                                    + " tai warehouseId=" + item.warehouseId()));
            if (inventory.getQuantity() == null || inventory.getQuantity() < item.quantity()) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                        "Kho local " + siteCode + " khong du hang cho productId=" + item.productId());
            }
            inventory.setQuantity(inventory.getQuantity() - item.quantity());
            inventoryRepo.save(inventory);

            details.add(OrderDetail.builder()
                    .id(OrderDetailId.builder()
                            .orderId(orderId)
                            .productId(item.productId())
                            .warehouseId(item.warehouseId())
                            .build())
                    .order(order)
                    .product(product)
                    .quantity(item.quantity())
                    .price(product.getPrice())
                    .build());
        }

        return switch (siteCodeEnum) {
            case DN -> orderPersistenceService.createOrderAtDanang(order, details);
            case HCM -> orderPersistenceService.createOrderAtHcm(order, details);
            default -> orderPersistenceService.createOrderAtHanoi(order, details);
        };
    }

    private List<LocalOrderLine> normalizeItems(LocalOrderRequest request) {
        validateRequest(request);
        Integer orderWarehouseId = request.getItems().get(0).getWarehouseId();
        Map<LocalOrderLineKey, Integer> quantityByLine = new LinkedHashMap<>();

        for (LocalOrderItemRequest item : request.getItems()) {
            if (!orderWarehouseId.equals(item.getWarehouseId())) {
                throw new AppException(ErrorCode.INVALID_KEY,
                        "Don hang local chi duoc lay hang tu mot kho");
            }
            LocalOrderLineKey key = new LocalOrderLineKey(item.getProductId(), item.getWarehouseId());
            quantityByLine.merge(key, item.getQuantity(), Integer::sum);
        }

        List<LocalOrderLine> normalizedItems = new ArrayList<>();
        for (Map.Entry<LocalOrderLineKey, Integer> entry : quantityByLine.entrySet()) {
            LocalOrderLineKey key = entry.getKey();
            normalizedItems.add(new LocalOrderLine(key.productId(), key.warehouseId(), entry.getValue()));
        }
        return normalizedItems;
    }

    private void verifyGlobalStockForLocalOrder(List<LocalOrderLine> items) {
        Map<Integer, Integer> requiredQuantityByProduct = new LinkedHashMap<>();
        for (LocalOrderLine item : items) {
            requiredQuantityByProduct.merge(item.productId(), item.quantity(), Integer::sum);
        }

        for (Map.Entry<Integer, Integer> entry : requiredQuantityByProduct.entrySet()) {
            Integer productId = entry.getKey();
            Integer requiredQuantity = entry.getValue();
            StockResponse globalStock = inventoryService.getGlobalStock(productId);
            int availableQuantity = globalStock.getQuantity() == null ? 0 : globalStock.getQuantity();
            if (availableQuantity < requiredQuantity) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                        "Ton kho toan he thong khong du cho productId=" + productId
                                + ", so luong can=" + requiredQuantity
                                + ", so luong kha dung=" + availableQuantity);
            }
        }
    }

    private void validateRequest(LocalOrderRequest request) {
        if (request == null || request.getCustomerId() == null) {
            throw new AppException(ErrorCode.INVALID_KEY, "Thiếu customerId");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY, "Đơn hàng phải có ít nhất một sản phẩm");
        }
        for (LocalOrderItemRequest item : request.getItems()) {
            if (item.getProductId() == null || item.getWarehouseId() == null
                    || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new AppException(ErrorCode.INVALID_KEY, "Thông tin item không hợp lệ");
            }
        }
    }

    private void validateWarehouseBelongsToSite(Warehouse warehouse, Integer siteId, String siteCode) {
        if (warehouse.getSite() == null || warehouse.getSite().getId() == null
                || !warehouse.getSite().getId().equals(siteId)) {
            throw new AppException(ErrorCode.INVALID_KEY,
                    "Warehouse không thuộc site local " + siteCode);
        }
    }

    private Site siteRef(Integer siteId, String siteCode) {
        return Site.builder().id(siteId).siteCode(siteCode).build();
    }

    private Long generateOrderId() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }

    private record LocalOrderLineKey(Integer productId, Integer warehouseId) {
    }

    private record LocalOrderLine(Integer productId, Integer warehouseId, Integer quantity) {
    }
}
