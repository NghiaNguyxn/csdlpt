package com.example.csdlpt.service;

import com.example.csdlpt.dto.request.LocalOrderItemRequest;
import com.example.csdlpt.dto.request.LocalOrderRequest;
import com.example.csdlpt.dto.request.MultiOrderItemRequest;
import com.example.csdlpt.dto.request.MultiOrderRequest;
import com.example.csdlpt.dto.request.OrderAllocationRequest;
import com.example.csdlpt.dto.request.OrderStatusRequest;
import com.example.csdlpt.dto.response.OrderDetailResponse;
import com.example.csdlpt.dto.response.OrderResponse;
import com.example.csdlpt.entity.CustomerIdentity;
import com.example.csdlpt.entity.Inventory;
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
import com.example.csdlpt.repository.common.DistributedInventoryRepository;
import com.example.csdlpt.repository.common.DistributedOrderDetailRepository;
import com.example.csdlpt.repository.common.DistributedOrderRepository;
import com.example.csdlpt.repository.common.DistributedTransactionLogRepository;
import com.example.csdlpt.repository.site_dn.DanangCustomerIdentityRepository;
import com.example.csdlpt.repository.site_dn.DanangOrderDetailRepository;
import com.example.csdlpt.repository.site_dn.DanangOrderRepository;
import com.example.csdlpt.repository.site_dn.DanangTransactionLogRepository;
import com.example.csdlpt.repository.site_hcm.HcmCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hcm.HcmOrderDetailRepository;
import com.example.csdlpt.repository.site_hcm.HcmOrderRepository;
import com.example.csdlpt.repository.site_hcm.HcmTransactionLogRepository;
import com.example.csdlpt.repository.site_hn.HanoiCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hn.HanoiOrderDetailRepository;
import com.example.csdlpt.repository.site_hn.HanoiOrderRepository;
import com.example.csdlpt.repository.site_hn.HanoiTransactionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderManagementService {
    private final SiteRoutingService siteRoutingService;

    private final HanoiOrderRepository hanoiOrderRepository;
    private final DanangOrderRepository danangOrderRepository;
    private final HcmOrderRepository hcmOrderRepository;

    private final HanoiOrderDetailRepository hanoiOrderDetailRepository;
    private final DanangOrderDetailRepository danangOrderDetailRepository;
    private final HcmOrderDetailRepository hcmOrderDetailRepository;

    private final HanoiTransactionLogRepository hanoiTransactionLogRepository;
    private final DanangTransactionLogRepository danangTransactionLogRepository;
    private final HcmTransactionLogRepository hcmTransactionLogRepository;

    private final HanoiCustomerIdentityRepository hanoiCustomerIdentityRepository;
    private final DanangCustomerIdentityRepository danangCustomerIdentityRepository;
    private final HcmCustomerIdentityRepository hcmCustomerIdentityRepository;

    private final TwoPhaseCommitStockService twoPhaseCommitStockService;

    @Transactional("hanoiTransactionManager")
    public OrderResponse createLocalHanoiOrder(LocalOrderRequest request) {
        validateLocalOrder(request);

        SiteCode localSite = SiteCode.HN;
        Long orderId = request.getOrderId() != null ? request.getOrderId() : generateOrderId(localSite);
        if (hanoiOrderRepository.existsById(orderId)) {
            throw new AppException(ErrorCode.INVALID_KEY, "Mã đơn hàng đã tồn tại tại HN: " + orderId);
        }

        CustomerIdentity customer = hanoiCustomerIdentityRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY,
                        "Không tìm thấy customer tại HN"));
        Warehouse warehouse = siteRoutingService.findWarehouseBySite(request.getWarehouseId(), localSite);

        Map<Integer, Integer> quantityByProduct = mergeLocalItems(request.getItems());
        for (Map.Entry<Integer, Integer> entry : quantityByProduct.entrySet()) {
            Integer productId = entry.getKey();
            Integer quantity = entry.getValue();


            int systemQuantity = getSystemQuantity(productId);
            if (systemQuantity < quantity) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                        "Tồn kho toàn hệ thống không đủ cho productId=" + productId);
            }


            siteRoutingService.findProductBySite(productId, localSite);
            int updated = siteRoutingService.getInventoryRepository(localSite)
                    .reduceStockIfEnough(warehouse.getId(), productId, quantity);
            if (updated == 0) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                        "Kho HN không đủ tồn kho cho productId=" + productId);
            }
        }

        Order order = Order.builder()
                .id(orderId)
                .customer(customer)
                .status(OrderStatus.COMPLETED)
                .site(warehouse.getSite())
                .build();
        hanoiOrderRepository.save(order);

        for (Map.Entry<Integer, Integer> entry : quantityByProduct.entrySet()) {
            Integer productId = entry.getKey();
            Integer quantity = entry.getValue();
            ProductBasic product = siteRoutingService.findProductBySite(productId, localSite);
            OrderDetail detail = OrderDetail.builder()
                    .id(new OrderDetailId(orderId, productId, warehouse.getId()))
                    .order(order)
                    .product(product)
                    .quantity(quantity)
                    .price(product.getPrice())
                    .build();
            hanoiOrderDetailRepository.save(detail);
        }

        return toResponse(hanoiOrderRepository.findById(orderId).orElse(order), localSite, null);
    }

    public OrderResponse createMultiWarehouseOrder(MultiOrderRequest request) {
        validateMultiOrder(request);
        SiteCode mainSite = parseSite(request.getMainSite());
        Long orderId = request.getOrderId() != null ? request.getOrderId() : System.currentTimeMillis();
        String txId = "TX-" + orderId + "-" + UUID.randomUUID();

        DistributedOrderRepository orderRepo = orderRepo(mainSite);
        DistributedOrderDetailRepository detailRepo = detailRepo(mainSite);
        DistributedTransactionLogRepository txRepo = txRepo(mainSite);
        if (orderRepo.existsById(orderId)) {
            throw new AppException(ErrorCode.INVALID_KEY, "Mã đơn hàng đã tồn tại tại site " + mainSite + ": " + orderId);
        }

        List<PreparedParticipant> preparedParticipants = new ArrayList<>();
        List<String> participantLogs = new ArrayList<>();
        txRepo.save(TransactionLog.builder()
                .transactionId(txId)
                .status(TransactionStatus.PREPARED)
                .participants("2PC_GLOBAL_PREPARE_STARTED")
                .build());

        try {
            // Phase 1: PREPARE. Mỗi site giữ tạm tồn kho bằng reserved_quantity và ghi participant log.
            for (MultiOrderItemRequest item : request.getItems()) {
                for (OrderAllocationRequest allocation : item.getAllocations()) {
                    SiteCode allocationSite = parseSite(allocation.getSiteCode());
                    siteRoutingService.findProductBySite(item.getProductId(), allocationSite);
                    siteRoutingService.findWarehouseBySite(allocation.getWarehouseId(), allocationSite);

                    twoPhaseCommitStockService.prepare(allocationSite, txId, allocation, item.getProductId());
                    preparedParticipants.add(new PreparedParticipant(allocationSite, item.getProductId(), allocation));
                    participantLogs.add(allocationSite + ":PREPARED(product=" + item.getProductId()
                            + ",warehouse=" + allocation.getWarehouseId()
                            + ",qty=" + allocation.getQuantity() + ")");
                }
            }

            CustomerIdentity customer = customerRepo(mainSite).findById(request.getCustomerId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY,
                            "Không tìm thấy customer tại site lưu đơn " + mainSite));
            Warehouse mainWarehouse = siteRoutingService.findWarehouseBySite(request.getMainWarehouseId(), mainSite);

            // Phase 2: GLOBAL COMMIT. Tất cả participant đã PREPARED nên trừ kho chính thức.
            for (PreparedParticipant participant : preparedParticipants) {
                twoPhaseCommitStockService.commit(
                        participant.site(), txId, participant.allocation(), participant.productId());
                participantLogs.add(participant.site() + ":COMMITTED(product=" + participant.productId()
                        + ",warehouse=" + participant.allocation().getWarehouseId()
                        + ",qty=" + participant.allocation().getQuantity() + ")");
            }

            Order order = Order.builder()
                    .id(orderId)
                    .customer(customer)
                    .status(OrderStatus.COMPLETED)
                    .site(mainWarehouse.getSite())
                    .build();
            orderRepo.save(order);

            for (MultiOrderItemRequest item : request.getItems()) {
                ProductBasic productAtMainSite = siteRoutingService.findProductBySite(item.getProductId(), mainSite);
                for (OrderAllocationRequest allocation : item.getAllocations()) {
                    OrderDetail detail = OrderDetail.builder()
                            .id(new OrderDetailId(orderId, item.getProductId(), allocation.getWarehouseId()))
                            .order(order)
                            .product(productAtMainSite)
                            .quantity(allocation.getQuantity())
                            .price(productAtMainSite.getPrice())
                            .build();
                    detailRepo.save(detail);
                }
            }

            updateTransaction(txRepo, txId, TransactionStatus.COMMITTED, String.join(" | ", participantLogs));
            return toResponse(orderRepo.findById(orderId).orElse(order), mainSite, txId);
        } catch (Exception exception) {
            // GLOBAL ABORT. Chỉ abort các participant đã PREPARED để giải phóng reserved_quantity.
            abortPreparedParticipants(txId, preparedParticipants, participantLogs);
            updateTransaction(txRepo, txId, TransactionStatus.ABORTED,
                    String.join(" | ", participantLogs) + " | ABORTED: " + exception.getMessage());
            if (exception instanceof AppException appException) {
                throw appException;
            }
            throw new AppException(ErrorCode.CREATE_FAILED, exception.getMessage());
        }
    }

    public List<OrderResponse> getOrders() {
        List<OrderResponse> responses = new ArrayList<>();
        for (SiteCode site : SiteCode.values()) {
            orderRepo(site).findAll().forEach(order -> responses.add(toResponse(order, site, null)));
        }
        responses.sort(Comparator.comparing(OrderResponse::getId));
        return responses;
    }

    public OrderResponse getOrderById(Long id) {
        for (SiteCode site : SiteCode.values()) {
            Optional<Order> order = orderRepo(site).findById(id);
            if (order.isPresent()) {
                return toResponse(order.get(), site, null);
            }
        }
        throw new AppException(ErrorCode.ORDER_NOT_FOUND);
    }

    public OrderResponse updateStatus(Long id, OrderStatusRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new AppException(ErrorCode.INVALID_KEY, "Trạng thái đơn hàng không hợp lệ");
        }
        for (SiteCode site : SiteCode.values()) {
            Optional<Order> optionalOrder = orderRepo(site).findById(id);
            if (optionalOrder.isPresent()) {
                Order order = optionalOrder.get();
                order.setStatus(request.getStatus());
                orderRepo(site).save(order);
                return toResponse(order, site, null);
            }
        }
        throw new AppException(ErrorCode.ORDER_NOT_FOUND);
    }

    private void abortPreparedParticipants(String txId,
                                           List<PreparedParticipant> preparedParticipants,
                                           List<String> participantLogs) {
        for (PreparedParticipant participant : preparedParticipants) {
            twoPhaseCommitStockService.abort(
                    participant.site(), txId, participant.allocation(), participant.productId());
            participantLogs.add(participant.site() + ":ABORTED(product=" + participant.productId()
                    + ",warehouse=" + participant.allocation().getWarehouseId()
                    + ",qty=" + participant.allocation().getQuantity() + ")");
        }
    }

    private void updateTransaction(DistributedTransactionLogRepository txRepo, String txId,
                                   TransactionStatus status, String participants) {
        TransactionLog tx = txRepo.findById(txId).orElse(TransactionLog.builder()
                .transactionId(txId)
                .build());
        tx.setStatus(status);
        tx.setParticipants(participants);
        txRepo.save(tx);
    }

    private OrderResponse toResponse(Order order, SiteCode sourceSite, String txId) {
        List<OrderDetailResponse> details = detailRepo(sourceSite).findByOrder_Id(order.getId()).stream()
                .map(detail -> toDetailResponse(detail, sourceSite))
                .toList();
        BigDecimal total = details.stream()
                .map(OrderDetailResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Integer mainWarehouseId = details.isEmpty() ? null : details.get(0).getWarehouseId();
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .warehouseId(mainWarehouseId)
                .siteId(order.getSite() != null ? order.getSite().getId() : null)
                .sourceSite(sourceSite.name())
                .totalAmount(total)
                .details(details)
                .transactionId(txId)
                .build();
    }

    private OrderDetailResponse toDetailResponse(OrderDetail detail, SiteCode sourceSite) {
        Integer productId = detail.getId().getProductId();
        Integer warehouseId = detail.getId().getWarehouseId();
        ProductBasic product = siteRoutingService.findProductBySite(productId, sourceSite);
        Warehouse warehouse = findWarehouseFromAnySite(warehouseId);
        BigDecimal lineTotal = detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
        return OrderDetailResponse.builder()
                .orderId(detail.getId().getOrderId())
                .productId(productId)
                .productName(product.getName())
                .warehouseId(warehouseId)
                .warehouseCode(warehouse.getCode())
                .quantity(detail.getQuantity())
                .price(detail.getPrice())
                .lineTotal(lineTotal)
                .build();
    }

    private Warehouse findWarehouseFromAnySite(Integer warehouseId) {
        for (SiteCode site : SiteCode.values()) {
            try {
                return siteRoutingService.findWarehouseBySite(warehouseId, site);
            } catch (RuntimeException ignored) {
                // Thử site tiếp theo vì theo schema main mỗi site chỉ giữ warehouse cục bộ.
            }
        }
        throw new AppException(ErrorCode.WAREHOUSE_NOT_FOUND, "Không tìm thấy kho id=" + warehouseId);
    }

    private void validateLocalOrder(LocalOrderRequest request) {
        if (request == null || request.getCustomerId() == null || request.getWarehouseId() == null
                || request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY, "Thiếu dữ liệu tạo đơn local HN");
        }
        if (request.getSiteCode() != null && !request.getSiteCode().isBlank()
                && !SiteCode.HN.name().equals(request.getSiteCode().trim().toUpperCase())) {
            throw new AppException(ErrorCode.INVALID_KEY,
                    "API /api/orders/local của Hiển chỉ xử lý đơn local tại HN");
        }
        for (LocalOrderItemRequest item : request.getItems()) {
            if (item == null || item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new AppException(ErrorCode.INVALID_KEY,
                        "Mỗi item local phải có productId và quantity > 0");
            }
        }
    }

    private Map<Integer, Integer> mergeLocalItems(List<LocalOrderItemRequest> items) {
        Map<Integer, Integer> quantityByProduct = new LinkedHashMap<>();
        for (LocalOrderItemRequest item : items) {
            quantityByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return quantityByProduct;
    }

    private int getSystemQuantity(Integer productId) {
        int total = 0;
        for (SiteCode site : SiteCode.values()) {
            List<Inventory> inventories = siteRoutingService.getInventoryRepository(site).findByProductId(productId);
            for (Inventory inventory : inventories) {
                total += inventory.getQuantity() == null ? 0 : inventory.getQuantity();
            }
        }
        return total;
    }

    private Long generateOrderId(SiteCode siteCode) {
        long prefix = switch (siteCode) {
            case DN -> 2_000_000_000_000L;
            case HCM -> 3_000_000_000_000L;
            default -> 1_000_000_000_000L;
        };
        long candidate = prefix + System.currentTimeMillis();
        while (orderRepo(siteCode).existsById(candidate)) {
            candidate++;
        }
        return candidate;
    }

    private void validateMultiOrder(MultiOrderRequest request) {
        if (request == null || request.getCustomerId() == null || request.getMainSite() == null
                || request.getMainWarehouseId() == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY, "Thiếu dữ liệu tạo đơn liên kho");
        }
        for (MultiOrderItemRequest item : request.getItems()) {
            if (item.getProductId() == null || item.getAllocations() == null || item.getAllocations().isEmpty()) {
                throw new AppException(ErrorCode.INVALID_KEY, "Mỗi item phải có productId và allocations");
            }
            for (OrderAllocationRequest allocation : item.getAllocations()) {
                if (allocation.getSiteCode() == null || allocation.getWarehouseId() == null
                        || allocation.getQuantity() == null || allocation.getQuantity() <= 0) {
                    throw new AppException(ErrorCode.INVALID_KEY, "Allocation không hợp lệ");
                }
            }
        }
    }

    private SiteCode parseSite(String siteCode) {
        try {
            return SiteCode.valueOf(siteCode.trim().toUpperCase());
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_KEY, "SiteCode không hợp lệ: " + siteCode);
        }
    }

    private DistributedOrderRepository orderRepo(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangOrderRepository;
            case HCM -> hcmOrderRepository;
            default -> hanoiOrderRepository;
        };
    }

    private DistributedOrderDetailRepository detailRepo(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangOrderDetailRepository;
            case HCM -> hcmOrderDetailRepository;
            default -> hanoiOrderDetailRepository;
        };
    }

    private DistributedTransactionLogRepository txRepo(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangTransactionLogRepository;
            case HCM -> hcmTransactionLogRepository;
            default -> hanoiTransactionLogRepository;
        };
    }

    private org.springframework.data.jpa.repository.JpaRepository<CustomerIdentity, Long> customerRepo(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangCustomerIdentityRepository;
            case HCM -> hcmCustomerIdentityRepository;
            default -> hanoiCustomerIdentityRepository;
        };
    }

    private record PreparedParticipant(SiteCode site, Integer productId, OrderAllocationRequest allocation) {
    }

}
