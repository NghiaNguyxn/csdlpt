package com.example.csdlpt.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.csdlpt.context.SiteContextHolder;
import com.example.csdlpt.dto.request.DistributedOrderItemRequest;
import com.example.csdlpt.dto.request.DistributedOrderRequest;
import com.example.csdlpt.dto.response.DistributedOrderResponse;
import com.example.csdlpt.dto.response.OrderAllocationResponse;
import com.example.csdlpt.entity.CustomerIdentity;
import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.entity.InventoryId;
import com.example.csdlpt.entity.Order;
import com.example.csdlpt.entity.OrderDetail;
import com.example.csdlpt.entity.OrderDetailId;
import com.example.csdlpt.entity.ProductBasic;
import com.example.csdlpt.entity.TransactionLog;
import com.example.csdlpt.enums.OrderStatus;
import com.example.csdlpt.enums.SiteCode;
import com.example.csdlpt.enums.TransactionStatus;
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
public class OrderService {

    SiteRoutingService siteRoutingService;
    InventoryTransactionService inventoryTransactionService;
    OrderPersistenceService orderPersistenceService;

    public DistributedOrderResponse placeDistributedOrder(DistributedOrderRequest request) {
        List<OrderItem> items = normalizeItems(request);

        SiteCode localSiteCode = SiteContextHolder.getCurrentSite();
        CustomerIdentity customer = siteRoutingService.findCustomerBySite(request.getCustomerId(), localSiteCode);
        Map<Integer, ProductBasic> productsById = loadProducts(items, localSiteCode);

        Long orderId = generateOrderId();
        String transactionId = "ORDER-" + orderId;

        List<OrderAllocation> allocations = allocateStock(items, localSiteCode);
        String participants = toParticipants(allocations);

        saveTransactionLog(localSiteCode, transactionId, TransactionStatus.PREPARED, participants);
        log.info("2PC: coordinator ghi PREPARED, transactionId={}, participants={}", transactionId, participants);

        List<OrderAllocation> preparedAllocations = new ArrayList<>();
        Order order = buildOrder(orderId, customer);
        List<OrderDetail> details = buildOrderDetails(orderId, order, allocations, productsById);

        try {
            prepareParticipants(transactionId, allocations, preparedAllocations, request.getSimulateVoteNoSite());
            createOrder(localSiteCode, order, details);
        } catch (RuntimeException ex) {
            saveTransactionLog(localSiteCode, transactionId, TransactionStatus.ABORTED, participants);
            abortParticipants(transactionId, preparedAllocations);
            AppException abortException = toAbortException(transactionId, preparedAllocations, ex);
            log.error("2PC: coordinator quyết định ABORT, transactionId={}, lý do={}",
                    transactionId, abortException.getMessage(), ex);
            throw abortException;
        }

        saveTransactionLog(localSiteCode, transactionId, TransactionStatus.COMMITTED, participants);
        log.info("2PC: coordinator quyết định COMMIT, transactionId={}", transactionId);

        commitParticipants(transactionId, preparedAllocations);

        return toResponse(orderId, transactionId, localSiteCode, OrderStatus.COMPLETED,
                TransactionStatus.COMMITTED, allocations);
    }

    private Order buildOrder(Long orderId, CustomerIdentity customer) {
        return Order.builder()
                .id(orderId)
                .customer(customer)
                .status(OrderStatus.COMPLETED)
                .site(customer.getMainSite())
                .build();
    }

    private List<OrderDetail> buildOrderDetails(
            Long orderId,
            Order order,
            List<OrderAllocation> allocations,
            Map<Integer, ProductBasic> productsById) {
        return allocations.stream()
                .map(allocation -> {
                    ProductBasic product = productsById.get(allocation.productId());
                    return OrderDetail.builder()
                            .id(OrderDetailId.builder()
                                    .orderId(orderId)
                                    .productId(allocation.productId())
                                    .warehouseId(allocation.warehouseId())
                                    .build())
                            .order(order)
                            .product(product)
                            .quantity(allocation.quantity())
                            .price(product.getPrice())
                            .build();
                })
                .toList();
    }

    private List<OrderItem> normalizeItems(DistributedOrderRequest request) {
        if (request.getCustomerId() == null) {
            throw new AppException(ErrorCode.INVALID_KEY, "Thông tin khách hàng không hợp lệ");
        }

        List<DistributedOrderItemRequest> requestItems = request.getItems();

        if (requestItems == null || requestItems.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY, "Đơn hàng phải có ít nhất một dòng sản phẩm");
        }

        Map<Integer, Integer> quantityByProduct = new LinkedHashMap<>();
        for (DistributedOrderItemRequest item : requestItems) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new AppException(ErrorCode.INVALID_KEY, "Thông tin sản phẩm trong đơn hàng không hợp lệ");
            }
            quantityByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        return quantityByProduct.entrySet().stream()
                .map(entry -> new OrderItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<Integer, ProductBasic> loadProducts(List<OrderItem> items, SiteCode localSiteCode) {
        Map<Integer, ProductBasic> productsById = new LinkedHashMap<>();
        for (OrderItem item : items) {
            productsById.put(item.productId(), siteRoutingService.findProductBySite(item.productId(), localSiteCode));
        }
        return productsById;
    }

    private List<OrderAllocation> allocateStock(List<OrderItem> items, SiteCode localSiteCode) {
        List<OrderAllocation> allocations = new ArrayList<>();
        for (OrderItem item : items) {
            allocations.addAll(allocateStock(item.productId(), item.quantity(), localSiteCode));
        }
        return allocations;
    }

    private List<OrderAllocation> allocateStock(Integer productId, Integer requestedQuantity, SiteCode localSiteCode) {
        List<SiteCode> siteOrder = new ArrayList<>();
        siteOrder.add(localSiteCode);
        for (SiteCode siteCode : SiteCode.values()) {
            if (siteCode != localSiteCode) {
                siteOrder.add(siteCode);
            }
        }

        int remaining = requestedQuantity;
        List<OrderAllocation> allocations = new ArrayList<>();

        for (SiteCode siteCode : siteOrder) {
            if (remaining == 0) {
                break;
            }

            List<Inventory> inventories = siteRoutingService.findInventoryByProductAndSite(productId, siteCode).stream()
                    .filter(inventory -> inventory.getQuantity() != null && inventory.getQuantity() > 0)
                    .sorted(Comparator.comparing(inventory -> inventory.getId().getWarehouseId()))
                    .toList();

            for (Inventory inventory : inventories) {
                if (remaining == 0) {
                    break;
                }

                int allocatedQuantity = Math.min(remaining, inventory.getQuantity());
                allocations.add(new OrderAllocation(
                        siteCode,
                        inventory.getId().getWarehouseId(),
                        productId,
                        allocatedQuantity));
                remaining -= allocatedQuantity;
            }
        }

        if (remaining > 0) {
            int availableQuantity = requestedQuantity - remaining;
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                    "Không đủ tồn kho khả dụng để tạo đơn hàng: sản phẩm id=" + productId
                            + ", số lượng cần=" + requestedQuantity
                            + ", số lượng có thể phân bổ=" + availableQuantity
                            + ", còn thiếu=" + remaining
                            + ". Giao dịch 2PC chưa được tạo nên không có participant cần ABORT.");
        }

        return allocations;
    }

    private void prepareParticipants(
            String transactionId,
            List<OrderAllocation> allocations,
            List<OrderAllocation> preparedAllocations,
            SiteCode simulateVoteNoSite) {
        for (OrderAllocation allocation : allocations) {
            InventoryId inventoryId = toInventoryId(allocation);

            if (allocation.siteCode() == simulateVoteNoSite) {
                String message = "Mô phỏng Vote NO tại site " + allocation.siteCode()
                        + " trong pha PREPARE của 2PC: transactionId=" + transactionId
                        + ", sản phẩm id=" + allocation.productId()
                        + ", kho id=" + allocation.warehouseId()
                        + ", số lượng cần giữ=" + allocation.quantity()
                        + ". Coordinator sẽ ghi ABORT và hoàn tác các participant đã PREPARED.";
                voteNo(transactionId, allocation, inventoryId, message);
                throw new AppException(ErrorCode.SITE_CONNECTION_ERROR, message);
            }

            switch (allocation.siteCode()) {
                case DN -> inventoryTransactionService.prepareReservationAtDanang(
                        transactionId, inventoryId, allocation.quantity());
                case HCM -> inventoryTransactionService.prepareReservationAtHcm(
                        transactionId, inventoryId, allocation.quantity());
                default -> inventoryTransactionService.prepareReservationAtHanoi(
                        transactionId, inventoryId, allocation.quantity());
            }

            preparedAllocations.add(allocation);
        }
    }

    private void commitParticipants(String transactionId, List<OrderAllocation> preparedAllocations) {
        for (OrderAllocation allocation : preparedAllocations) {
            InventoryId inventoryId = toInventoryId(allocation);

            switch (allocation.siteCode()) {
                case DN -> inventoryTransactionService.commitReservationAtDanang(
                        transactionId, inventoryId, allocation.quantity());
                case HCM -> inventoryTransactionService.commitReservationAtHcm(
                        transactionId, inventoryId, allocation.quantity());
                default -> inventoryTransactionService.commitReservationAtHanoi(
                        transactionId, inventoryId, allocation.quantity());
            }
        }
    }

    private void abortParticipants(String transactionId, List<OrderAllocation> preparedAllocations) {
        for (OrderAllocation allocation : preparedAllocations) {
            try {
                InventoryId inventoryId = toInventoryId(allocation);

                switch (allocation.siteCode()) {
                    case DN -> inventoryTransactionService.abortReservationAtDanang(
                            transactionId, inventoryId, allocation.quantity());
                    case HCM -> inventoryTransactionService.abortReservationAtHcm(
                            transactionId, inventoryId, allocation.quantity());
                    default -> inventoryTransactionService.abortReservationAtHanoi(
                            transactionId, inventoryId, allocation.quantity());
                }
            } catch (RuntimeException abortError) {
                log.error("Không thể ABORT participant đã PREPARED, allocation={}", allocation, abortError);
            }
        }
    }

    private AppException toAbortException(
            String transactionId,
            List<OrderAllocation> preparedAllocations,
            RuntimeException ex) {
        ErrorCode errorCode = ex instanceof AppException appException
                ? appException.getErrorCode()
                : ErrorCode.ORDER_ABORTED;

        String preparedParticipants = preparedAllocations.isEmpty()
                ? "không có participant nào đã PREPARED"
                : preparedAllocations.stream()
                        .map(allocation -> allocation.siteCode().name()
                                + "(warehouseId=" + allocation.warehouseId()
                                + ", productId=" + allocation.productId()
                                + ", quantity=" + allocation.quantity() + ")")
                        .collect(Collectors.joining(", "));

        String reason = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "không xác định"
                : ex.getMessage();

        return new AppException(errorCode,
                "Giao dịch 2PC đã ABORT: transactionId=" + transactionId
                        + ". Lý do: " + reason
                        + ". Các participant đã PREPARED đã được yêu cầu hoàn tác: "
                        + preparedParticipants + ".");
    }

    private void voteNo(String transactionId, OrderAllocation allocation, InventoryId inventoryId, String message) {
        switch (allocation.siteCode()) {
            case DN -> inventoryTransactionService.voteNoAtDanang(
                    transactionId, inventoryId, allocation.quantity(), message);
            case HCM -> inventoryTransactionService.voteNoAtHcm(
                    transactionId, inventoryId, allocation.quantity(), message);
            default -> inventoryTransactionService.voteNoAtHanoi(
                    transactionId, inventoryId, allocation.quantity(), message);
        }
    }

    private InventoryId toInventoryId(OrderAllocation allocation) {
        return InventoryId.builder()
                .warehouseId(allocation.warehouseId())
                .productId(allocation.productId())
                .build();
    }

    private void saveTransactionLog(
            SiteCode siteCode,
            String transactionId,
            TransactionStatus status,
            String participants) {
        TransactionLog transactionLog = TransactionLog.builder()
                .transactionId(transactionId)
                .status(status)
                .participants(participants)
                .build();

        switch (siteCode) {
            case DN -> orderPersistenceService.saveTransactionLogAtDanang(transactionLog);
            case HCM -> orderPersistenceService.saveTransactionLogAtHcm(transactionLog);
            default -> orderPersistenceService.saveTransactionLogAtHanoi(transactionLog);
        }
    }

    private void createOrder(SiteCode siteCode, Order order, List<OrderDetail> details) {
        switch (siteCode) {
            case DN -> orderPersistenceService.createOrderAtDanang(order, details);
            case HCM -> orderPersistenceService.createOrderAtHcm(order, details);
            default -> orderPersistenceService.createOrderAtHanoi(order, details);
        }
    }

    private DistributedOrderResponse toResponse(
            Long orderId,
            String transactionId,
            SiteCode localSiteCode,
            OrderStatus orderStatus,
            TransactionStatus transactionStatus,
            List<OrderAllocation> allocations) {
        return DistributedOrderResponse.builder()
                .orderId(orderId)
                .transactionId(transactionId)
                .localSiteCode(localSiteCode)
                .orderStatus(orderStatus)
                .transactionStatus(transactionStatus)
                .allocations(allocations.stream()
                        .map(allocation -> OrderAllocationResponse.builder()
                                .siteCode(allocation.siteCode())
                                .warehouseId(allocation.warehouseId())
                                .productId(allocation.productId())
                                .quantity(allocation.quantity())
                                .build())
                        .toList())
                .build();
    }

    private String toParticipants(List<OrderAllocation> allocations) {
        return allocations.stream()
                .map(allocation -> allocation.siteCode().name()
                        + ":warehouse=" + allocation.warehouseId()
                        + ":product=" + allocation.productId()
                        + ":quantity=" + allocation.quantity())
                .collect(Collectors.joining(","));
    }

    private Long generateOrderId() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }

    private record OrderItem(Integer productId, Integer quantity) {
    }

    private record OrderAllocation(SiteCode siteCode, Integer warehouseId, Integer productId, Integer quantity) {
    }
}
