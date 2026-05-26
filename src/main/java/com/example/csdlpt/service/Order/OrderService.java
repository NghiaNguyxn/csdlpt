package com.example.csdlpt.service.Order;

import java.math.BigDecimal;
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
import com.example.csdlpt.dto.request.LocalOrderRequest;
import com.example.csdlpt.dto.request.OrderStatusRequest;
import com.example.csdlpt.dto.response.DistributedOrderResponse;
import com.example.csdlpt.dto.response.OrderAllocationResponse;
import com.example.csdlpt.dto.response.OrderDetailResponse;
import com.example.csdlpt.dto.response.OrderResponse;
import com.example.csdlpt.entity.CustomerIdentity;
import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.entity.InventoryId;
import com.example.csdlpt.entity.Order;
import com.example.csdlpt.entity.OrderDetail;
import com.example.csdlpt.entity.OrderDetailId;
import com.example.csdlpt.entity.ProductBasic;
import com.example.csdlpt.entity.TransactionLog;
import com.example.csdlpt.entity.Warehouse;
import com.example.csdlpt.enums.OrderStatus;
import com.example.csdlpt.enums.SiteCode;
import com.example.csdlpt.enums.TransactionStatus;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.service.InventoryTransactionService;
import com.example.csdlpt.service.SiteRoutingService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderService {

    private static final int MAX_REALLOCATION_ATTEMPTS = 2;
    private static final String GLOBAL_STOCK_NOT_ENOUGH_MESSAGE =
            "Không đủ tồn kho trên toàn hệ thống để đáp ứng đơn hàng. Vui lòng giảm số lượng hoặc chọn sản phẩm khác.";
    private static final String STOCK_CHANGED_DURING_PROCESSING_MESSAGE =
            "Đơn hàng chưa thể hoàn tất vì tồn kho đã thay đổi trong lúc xử lý. "
                    + "Hệ thống đã thử phân bổ lại nhưng chưa giữ được đủ hàng. Vui lòng thử lại.";

    SiteRoutingService siteRoutingService;
    InventoryTransactionService inventoryTransactionService;
    OrderPersistenceService orderPersistenceService;
    LocalOrderTransactionService localOrderTransactionService;

    public OrderResponse createLocalOrder(LocalOrderRequest request) {
        SiteCode localSite = currentSite();
        Order saved = switch (localSite) {
            case DN -> localOrderTransactionService.createLocalOrderAtDanang(request);
            case HCM -> localOrderTransactionService.createLocalOrderAtHcm(request);
            default -> localOrderTransactionService.createLocalOrderAtHanoi(request);
        };
        return getOrderById(saved.getId());
    }

    public OrderResponse getOrderById(Long id) {
        OrderSite found = findOrderSite(id);
        return toOrderResponse(found.order(), found.siteCode(), true);
    }

    public List<OrderResponse> getAllOrders(String status) {
        OrderStatus resolvedStatus = resolveOrderStatusFilter(status);
        return List.of(
                        findOrdersBySite(SiteCode.HN, resolvedStatus).stream()
                                .map(order -> toOrderResponse(order, SiteCode.HN, false)).toList(),
                        findOrdersBySite(SiteCode.DN, resolvedStatus).stream()
                                .map(order -> toOrderResponse(order, SiteCode.DN, false)).toList(),
                        findOrdersBySite(SiteCode.HCM, resolvedStatus).stream()
                                .map(order -> toOrderResponse(order, SiteCode.HCM, false)).toList())
                .stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparing(OrderResponse::getOrderDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<OrderResponse> getAllOrdersBySite(String siteCode, String status) {
        SiteCode resolvedSite = resolveSiteCode(siteCode);
        OrderStatus resolvedStatus = resolveOrderStatusFilter(status);
        return findOrdersBySite(resolvedSite, resolvedStatus).stream()
                .map(order -> toOrderResponse(order, resolvedSite, false))
                .sorted(Comparator.comparing(OrderResponse::getOrderDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public OrderResponse updateStatus(Long id, OrderStatusRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new AppException(ErrorCode.INVALID_KEY, "Trạng thái đơn hàng không hợp lệ");
        }
        OrderSite found = findOrderSite(id);
        Order updated = switch (found.siteCode()) {
            case DN -> localOrderTransactionService.updateStatusAtDanang(id, request.getStatus());
            case HCM -> localOrderTransactionService.updateStatusAtHcm(id, request.getStatus());
            default -> localOrderTransactionService.updateStatusAtHanoi(id, request.getStatus());
        };
        return toOrderResponse(updated, found.siteCode(), true);
    }

    public DistributedOrderResponse placeDistributedOrder(DistributedOrderRequest request) {
        List<OrderItem> items = normalizeItems(request);

        SiteCode localSiteCode = SiteContextHolder.getCurrentSite();
        CustomerIdentity customer = siteRoutingService.findCustomerBySite(request.getCustomerId(), localSiteCode);
        Map<Integer, ProductBasic> productsById = loadProducts(items, localSiteCode);

        int maxAttempts = request.getSimulateVoteNoSite() == null ? MAX_REALLOCATION_ATTEMPTS : 1;
        AppException lastAbortException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return placeDistributedOrderAttempt(request, items, localSiteCode, customer, productsById,
                        attempt, maxAttempts);
            } catch (StockAllocationException ex) {
                log.warn("Không đủ tồn kho toàn hệ thống ở bước phân bổ. Lý do kỹ thuật: {}", safeMessage(ex));
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK, GLOBAL_STOCK_NOT_ENOUGH_MESSAGE);
            } catch (StockChangedDuringPrepareException ex) {
                AppException userException = new AppException(ErrorCode.INSUFFICIENT_STOCK,
                        STOCK_CHANGED_DURING_PROCESSING_MESSAGE);
                if (!shouldRetryWithReallocation(request, attempt, maxAttempts)) {
                    throw userException;
                }

                lastAbortException = userException;
                log.warn("Đơn hàng bị tranh chấp tồn kho ở attempt {}/{}. Hệ thống sẽ đọc lại tồn kho và phân bổ lại. Lý do: {}",
                        attempt, maxAttempts, safeMessage(ex));
            } catch (AppException ex) {
                throw ex;
            }
        }

        throw lastAbortException;
    }

    private DistributedOrderResponse placeDistributedOrderAttempt(
            DistributedOrderRequest request,
            List<OrderItem> items,
            SiteCode localSiteCode,
            CustomerIdentity customer,
            Map<Integer, ProductBasic> productsById,
            int attempt,
            int maxAttempts) {
        Long orderId = generateOrderId();
        String transactionId = "ORDER-" + orderId;

        List<OrderAllocation> allocations = allocateStock(items, localSiteCode);
        String participants = toParticipants(allocations);

        // Coordinator log: bắt đầu pha 1 của 2PC và ghi danh sách participant dự kiến.
        saveTransactionLog(localSiteCode, transactionId, TransactionStatus.PREPARED, participants);
        log.info("2PC: coordinator ghi PREPARED, transactionId={}, attempt={}/{}, participants={}",
                transactionId, attempt, maxAttempts, participants);

        List<OrderAllocation> preparedAllocations = new ArrayList<>();
        // Order giữ PENDING cho tới khi mọi participant áp dụng Global COMMIT thành công.
        Order order = buildOrder(orderId, customer, OrderStatus.PENDING);
        List<OrderDetail> details = buildOrderDetails(orderId, order, allocations, productsById);

        try {
            // Phase 1: gửi PREPARE tới từng participant. Chỉ participant đã Vote YES mới được abort nếu lỗi.
            prepareParticipants(transactionId, allocations, preparedAllocations, request.getSimulateVoteNoSite());
            createOrder(localSiteCode, order, details);
        } catch (RuntimeException ex) {
            saveTransactionLog(localSiteCode, transactionId, TransactionStatus.ABORTED, participants);
            abortParticipants(transactionId, preparedAllocations);
            AppException abortException = toAbortException(transactionId, preparedAllocations, ex);
            log.error("2PC: coordinator quyết định ABORT, transactionId={}, attempt={}/{}, lý do={}",
                    transactionId, attempt, maxAttempts, abortException.getMessage(), ex);
            if (abortException.getErrorCode() == ErrorCode.INSUFFICIENT_STOCK) {
                throw new StockChangedDuringPrepareException(abortException);
            }
            throw abortException;
        }

        saveTransactionLog(localSiteCode, transactionId, TransactionStatus.COMMITTED, participants);
        log.info("2PC: coordinator quyết định COMMIT, transactionId={}", transactionId);

        try {
            // Phase 2: sau Global COMMIT không được ABORT nữa; lỗi ở đây cần retry commit/recovery.
            commitParticipants(transactionId, preparedAllocations);
        } catch (RuntimeException ex) {
            throw new AppException(ErrorCode.SITE_CONNECTION_ERROR,
                    "Giao dịch 2PC đã có quyết định COMMIT nhưng chưa áp dụng xong ở tất cả participant: "
                            + "transactionId=" + transactionId
                            + ". Không được ABORT giao dịch này; cần chạy lại thao tác COMMIT/kiểm tra participant log. "
                            + "Lý do: " + safeMessage(ex));
        }

        return toResponse(orderId, transactionId, localSiteCode, OrderStatus.PENDING,
                TransactionStatus.COMMITTED, allocations);
    }

    private boolean shouldRetryWithReallocation(
            DistributedOrderRequest request,
            int attempt,
            int maxAttempts) {
        return request.getSimulateVoteNoSite() == null
                && attempt < maxAttempts;
    }

    private Order buildOrder(Long orderId, CustomerIdentity customer, OrderStatus status) {
        return Order.builder()
                .id(orderId)
                .customer(customer)
                .status(status)
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

        // Ưu tiên kho ở site local, sau đó mới lấy bù từ các site khác để giảm chi phí phân tán.
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
            throw new StockAllocationException(
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

            // simulateVoteNoSite chỉ phục vụ demo nhánh ABORT của 2PC, không phải logic nghiệp vụ thật.
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

        return new AppException(errorCode,
                "Giao dịch 2PC đã ABORT: transactionId=" + transactionId
                        + ". Lý do: " + safeMessage(ex)
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

    private void updateOrderStatus(SiteCode siteCode, Long orderId, OrderStatus status) {
        switch (siteCode) {
            case DN -> orderPersistenceService.updateOrderStatusAtDanang(orderId, status);
            case HCM -> orderPersistenceService.updateOrderStatusAtHcm(orderId, status);
            default -> orderPersistenceService.updateOrderStatusAtHanoi(orderId, status);
        }
    }

    private OrderSite findOrderSite(Long id) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_KEY, "orderId không hợp lệ");
        }
        return orderPersistenceService.findOrderAtHanoi(id).map(order -> new OrderSite(order, SiteCode.HN))
                .or(() -> orderPersistenceService.findOrderAtDanang(id).map(order -> new OrderSite(order, SiteCode.DN)))
                .or(() -> orderPersistenceService.findOrderAtHcm(id).map(order -> new OrderSite(order, SiteCode.HCM)))
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
    }

    private List<Order> findAllOrdersBySite(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> orderPersistenceService.findAllOrdersAtDanang();
            case HCM -> orderPersistenceService.findAllOrdersAtHcm();
            default -> orderPersistenceService.findAllOrdersAtHanoi();
        };
    }

    private List<Order> findOrdersBySite(SiteCode siteCode, OrderStatus status) {
        if (status == null) {
            return findAllOrdersBySite(siteCode);
        }
        return switch (siteCode) {
            case DN -> orderPersistenceService.findOrdersByStatusAtDanang(status);
            case HCM -> orderPersistenceService.findOrdersByStatusAtHcm(status);
            default -> orderPersistenceService.findOrdersByStatusAtHanoi(status);
        };
    }

    private SiteCode resolveSiteCode(String siteCode) {
        if (siteCode == null || siteCode.isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY, "siteCode khong hop le, chi nhan HN, DN, HCM");
        }
        try {
            return SiteCode.valueOf(siteCode.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.INVALID_KEY, "siteCode khong hop le, chi nhan HN, DN, HCM");
        }
    }

    private OrderStatus resolveOrderStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.INVALID_KEY,
                    "status không hợp lệ, chỉ nhận PENDING, COMPLETED, CANCELLED");
        }
    }

    private OrderResponse toOrderResponse(Order order, SiteCode siteCode, boolean includeDetails) {
        List<OrderDetail> details = detailsByOrder(order.getId(), siteCode);
        BigDecimal totalAmount = details.stream()
                .map(detail -> safe(detail.getPrice()).multiply(BigDecimal.valueOf(detail.getQuantity() == null ? 0 : detail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomer() == null ? null : order.getCustomer().getId())
                .orderDate(order.getOrderDate())
                .status(order.getStatus() == null ? OrderStatus.PENDING : order.getStatus())
                .siteCode(siteCode.name())
                .sourceSite(siteCode.name())
                .totalAmount(totalAmount)
                .details(includeDetails ? details.stream().map(detail -> toOrderDetailResponse(detail, siteCode)).toList() : null)
                .build();
    }

    private OrderDetailResponse toOrderDetailResponse(OrderDetail detail, SiteCode siteCode) {
        Integer productId = detail.getId() == null ? null : detail.getId().getProductId();
        Integer warehouseId = detail.getId() == null ? null : detail.getId().getWarehouseId();
        ProductBasic product = productId == null ? null : productById(productId, siteCode);
        Warehouse warehouse = warehouseId == null ? null : warehouseById(warehouseId, siteCode);
        BigDecimal price = safe(detail.getPrice());
        Integer quantity = detail.getQuantity() == null ? 0 : detail.getQuantity();
        return OrderDetailResponse.builder()
                .orderId(detail.getId() == null ? null : detail.getId().getOrderId())
                .productId(productId)
                .productName(product == null ? null : product.getName())
                .warehouseId(warehouseId)
                .warehouseCode(warehouse == null ? null : warehouse.getCode())
                .quantity(quantity)
                .price(price)
                .lineTotal(price.multiply(BigDecimal.valueOf(quantity)))
                .build();
    }

    private List<OrderDetail> detailsByOrder(Long orderId, SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> orderPersistenceService.findOrderDetailsAtDanang(orderId);
            case HCM -> orderPersistenceService.findOrderDetailsAtHcm(orderId);
            default -> orderPersistenceService.findOrderDetailsAtHanoi(orderId);
        };
    }

    private Warehouse warehouseById(Integer warehouseId, SiteCode siteCode) {
        try {
            return siteRoutingService.findWarehouseBySite(warehouseId, siteCode);
        } catch (AppException ex) {
            return null;
        }
    }

    private ProductBasic productById(Integer productId, SiteCode siteCode) {
        try {
            return siteRoutingService.findProductBySite(productId, siteCode);
        } catch (AppException ex) {
            return null;
        }
    }

    private SiteCode currentSite() {
        return SiteContextHolder.getCurrentSite() == null ? SiteCode.HN : SiteContextHolder.getCurrentSite();
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

    private String safeMessage(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? "không xác định"
                : ex.getMessage();
    }

    private Long generateOrderId() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record OrderItem(Integer productId, Integer quantity) {
    }

    private record OrderAllocation(SiteCode siteCode, Integer warehouseId, Integer productId, Integer quantity) {
    }

    private record OrderSite(Order order, SiteCode siteCode) {
    }

    private static class StockAllocationException extends RuntimeException {
        StockAllocationException(String message) {
            super(message);
        }
    }

    private static class StockChangedDuringPrepareException extends RuntimeException {
        StockChangedDuringPrepareException(Throwable cause) {
            super(cause.getMessage(), cause);
        }
    }
}
