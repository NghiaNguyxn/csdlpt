package com.example.csdlpt.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.csdlpt.dto.response.InventoryResponse;
import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.entity.InventoryId;
import com.example.csdlpt.entity.ProductBasic;
import com.example.csdlpt.entity.TransactionParticipantLog;
import com.example.csdlpt.entity.Warehouse;
import com.example.csdlpt.enums.TransactionStatus;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.mapper.InventoryMapper;
import com.example.csdlpt.repository.InventoryLockingRepository;
import com.example.csdlpt.repository.site_dn.DanangInventoryRepository;
import com.example.csdlpt.repository.site_dn.DanangTransactionParticipantLogRepository;
import com.example.csdlpt.repository.site_hcm.HcmInventoryRepository;
import com.example.csdlpt.repository.site_hcm.HcmTransactionParticipantLogRepository;
import com.example.csdlpt.repository.site_hn.HanoiInventoryRepository;
import com.example.csdlpt.repository.site_hn.HanoiTransactionParticipantLogRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryTransactionService {

    HanoiInventoryRepository hanoiInventoryRepository;
    DanangInventoryRepository danangInventoryRepository;
    HcmInventoryRepository hcmInventoryRepository;
    HanoiTransactionParticipantLogRepository hanoiParticipantLogRepository;
    DanangTransactionParticipantLogRepository danangParticipantLogRepository;
    HcmTransactionParticipantLogRepository hcmParticipantLogRepository;
    InventoryMapper inventoryMapper;

    @Transactional("hanoiTransactionManager")
    public InventoryResponse addStockAtHanoi(
            InventoryId inventoryId,
            ProductBasic product,
            Warehouse warehouse,
            Integer quantity) {
        return addStockWithLock("HN", hanoiInventoryRepository, inventoryId, product, warehouse, quantity);
    }

    @Transactional("danangTransactionManager")
    public InventoryResponse addStockAtDanang(
            InventoryId inventoryId,
            ProductBasic product,
            Warehouse warehouse,
            Integer quantity) {
        return addStockWithLock("DN", danangInventoryRepository, inventoryId, product, warehouse, quantity);
    }

    @Transactional("hcmTransactionManager")
    public InventoryResponse addStockAtHcm(
            InventoryId inventoryId,
            ProductBasic product,
            Warehouse warehouse,
            Integer quantity) {
        return addStockWithLock("HCM", hcmInventoryRepository, inventoryId, product, warehouse, quantity);
    }

    @Transactional("hanoiTransactionManager")
    public InventoryResponse reduceStockAtHanoi(InventoryId inventoryId, Integer quantity) {
        return reduceStockWithLock("HN", hanoiInventoryRepository, inventoryId, quantity);
    }

    @Transactional("danangTransactionManager")
    public InventoryResponse reduceStockAtDanang(InventoryId inventoryId, Integer quantity) {
        return reduceStockWithLock("DN", danangInventoryRepository, inventoryId, quantity);
    }

    @Transactional("hcmTransactionManager")
    public InventoryResponse reduceStockAtHcm(InventoryId inventoryId, Integer quantity) {
        return reduceStockWithLock("HCM", hcmInventoryRepository, inventoryId, quantity);
    }

    @Transactional(value = "hanoiTransactionManager", noRollbackFor = AppException.class)
    public void prepareReservationAtHanoi(String transactionId, InventoryId inventoryId, Integer quantity) {
        prepareReservationWithLock(
                "HN", hanoiInventoryRepository, hanoiParticipantLogRepository, transactionId, inventoryId, quantity);
    }

    @Transactional(value = "danangTransactionManager", noRollbackFor = AppException.class)
    public void prepareReservationAtDanang(String transactionId, InventoryId inventoryId, Integer quantity) {
        prepareReservationWithLock(
                "DN", danangInventoryRepository, danangParticipantLogRepository, transactionId, inventoryId, quantity);
    }

    @Transactional(value = "hcmTransactionManager", noRollbackFor = AppException.class)
    public void prepareReservationAtHcm(String transactionId, InventoryId inventoryId, Integer quantity) {
        prepareReservationWithLock(
                "HCM", hcmInventoryRepository, hcmParticipantLogRepository, transactionId, inventoryId, quantity);
    }

    @Transactional("hanoiTransactionManager")
    public void commitReservationAtHanoi(String transactionId, InventoryId inventoryId, Integer quantity) {
        commitReservationWithLock(
                "HN", hanoiInventoryRepository, hanoiParticipantLogRepository, transactionId, inventoryId, quantity);
    }

    @Transactional("danangTransactionManager")
    public void commitReservationAtDanang(String transactionId, InventoryId inventoryId, Integer quantity) {
        commitReservationWithLock(
                "DN", danangInventoryRepository, danangParticipantLogRepository, transactionId, inventoryId, quantity);
    }

    @Transactional("hcmTransactionManager")
    public void commitReservationAtHcm(String transactionId, InventoryId inventoryId, Integer quantity) {
        commitReservationWithLock(
                "HCM", hcmInventoryRepository, hcmParticipantLogRepository, transactionId, inventoryId, quantity);
    }

    @Transactional("hanoiTransactionManager")
    public void abortReservationAtHanoi(String transactionId, InventoryId inventoryId, Integer quantity) {
        abortReservationWithLock(
                "HN", hanoiInventoryRepository, hanoiParticipantLogRepository, transactionId, inventoryId, quantity);
    }

    @Transactional("danangTransactionManager")
    public void abortReservationAtDanang(String transactionId, InventoryId inventoryId, Integer quantity) {
        abortReservationWithLock(
                "DN", danangInventoryRepository, danangParticipantLogRepository, transactionId, inventoryId, quantity);
    }

    @Transactional("hcmTransactionManager")
    public void abortReservationAtHcm(String transactionId, InventoryId inventoryId, Integer quantity) {
        abortReservationWithLock(
                "HCM", hcmInventoryRepository, hcmParticipantLogRepository, transactionId, inventoryId, quantity);
    }

    @Transactional("hanoiTransactionManager")
    public void voteNoAtHanoi(String transactionId, InventoryId inventoryId, Integer quantity, String message) {
        saveParticipantLog("HN", hanoiParticipantLogRepository, transactionId, inventoryId, quantity,
                TransactionStatus.ABORTED, message);
    }

    @Transactional("danangTransactionManager")
    public void voteNoAtDanang(String transactionId, InventoryId inventoryId, Integer quantity, String message) {
        saveParticipantLog("DN", danangParticipantLogRepository, transactionId, inventoryId, quantity,
                TransactionStatus.ABORTED, message);
    }

    @Transactional("hcmTransactionManager")
    public void voteNoAtHcm(String transactionId, InventoryId inventoryId, Integer quantity, String message) {
        saveParticipantLog("HCM", hcmParticipantLogRepository, transactionId, inventoryId, quantity,
                TransactionStatus.ABORTED, message);
    }

    private InventoryResponse addStockWithLock(
            String site,
            InventoryLockingRepository repository,
            InventoryId inventoryId,
            ProductBasic product,
            Warehouse warehouse,
            Integer quantity) {

        log.info("{} cần khóa cho addStock với id: {}", site, inventoryId);

        Inventory inventory = repository.findByIdForUpdate(inventoryId)
                .orElseGet(() -> Inventory.builder()
                        .id(inventoryId)
                        .product(product)
                        .warehouse(warehouse)
                        .quantity(0)
                        .build());

        inventory.setQuantity(inventory.getQuantity() + quantity);

        Inventory saved = repository.save(inventory);
        log.info("{} addStock thành công, nhả khóa với id: {}", site, inventoryId);

        return inventoryMapper.toResponse(saved);
    }

    private InventoryResponse reduceStockWithLock(
            String site,
            InventoryLockingRepository repository,
            InventoryId inventoryId,
            Integer quantity) {

        log.info("{} cần khóa cho reduceStock với id: {}", site, inventoryId);

        Inventory inventory = repository.findByIdForUpdate(inventoryId)
                .orElseThrow(() -> new AppException(ErrorCode.INSUFFICIENT_STOCK,
                        "Không tìm thấy tồn kho tại site " + site
                                + ": warehouseId=" + inventoryId.getWarehouseId()
                                + ", productId=" + inventoryId.getProductId()
                                + ". Không thể trừ hàng."));

        if (inventory.getQuantity() < quantity) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                    "Không đủ tồn kho khả dụng tại site " + site
                            + ": warehouseId=" + inventoryId.getWarehouseId()
                            + ", productId=" + inventoryId.getProductId()
                            + ", số lượng cần trừ=" + quantity
                            + ", số lượng khả dụng=" + inventory.getQuantity() + ".");
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);

        Inventory saved = repository.save(inventory);
        log.info("{} reduceStock thành công, nhả khóa với id: {}", site, inventoryId);

        return inventoryMapper.toResponse(saved);
    }

    private void prepareReservationWithLock(
            String site,
            InventoryLockingRepository inventoryRepository,
            JpaRepository<TransactionParticipantLog, Integer> logRepository,
            String transactionId,
            InventoryId inventoryId,
            Integer quantity) {

        log.info("{} cần khóa cho 2PC PREPARE với id: {}, transactionId={}", site, inventoryId, transactionId);

        // PREPARE dùng pessimistic lock cục bộ, rồi chuyển hàng khả dụng sang reserved_quantity.
        Inventory inventory = inventoryRepository.findByIdForUpdate(inventoryId)
                .orElseThrow(() -> {
                    String message = "Vote NO tại site " + site
                            + ": không tìm thấy tồn kho trong pha PREPARE"
                            + ", transactionId=" + transactionId
                            + ", warehouseId=" + inventoryId.getWarehouseId()
                            + ", productId=" + inventoryId.getProductId()
                            + ", số lượng cần giữ=" + quantity
                            + ". Coordinator sẽ ABORT giao dịch 2PC.";
                    saveParticipantLog(site, logRepository, transactionId, inventoryId, quantity,
                            TransactionStatus.ABORTED, message);
                    return new AppException(ErrorCode.INSUFFICIENT_STOCK, message);
                });

        if (inventory.getQuantity() < quantity) {
            String message = "Vote NO tại site " + site
                    + ": không đủ tồn kho khả dụng trong pha PREPARE"
                    + ", transactionId=" + transactionId
                    + ", warehouseId=" + inventoryId.getWarehouseId()
                    + ", productId=" + inventoryId.getProductId()
                    + ", số lượng cần giữ=" + quantity
                    + ", số lượng khả dụng=" + inventory.getQuantity()
                    + ". Coordinator sẽ ABORT giao dịch 2PC.";
            saveParticipantLog(site, logRepository, transactionId, inventoryId, quantity,
                    TransactionStatus.ABORTED, message);
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK, message);
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventory.setReservedQuantity(getReservedQuantity(inventory) + quantity);
        inventoryRepository.save(inventory);

        // Participant log là bằng chứng site này đã Vote YES và có hàng đang được giữ chỗ.
        saveParticipantLog(site, logRepository, transactionId, inventoryId, quantity,
                TransactionStatus.PREPARED, "Vote YES");
        log.info("{} 2PC PREPARE thành công, transactionId={}, id={}", site, transactionId, inventoryId);
    }

    private void commitReservationWithLock(
            String site,
            InventoryLockingRepository inventoryRepository,
            JpaRepository<TransactionParticipantLog, Integer> logRepository,
            String transactionId,
            InventoryId inventoryId,
            Integer quantity) {

        log.info("{} cần khóa cho 2PC COMMIT với id: {}, transactionId={}", site, inventoryId, transactionId);

        // COMMIT xác nhận bán hàng: reserved_quantity giảm, quantity không cộng lại.
        Inventory inventory = inventoryRepository.findByIdForUpdate(inventoryId)
                .orElseThrow(() -> new AppException(ErrorCode.INSUFFICIENT_STOCK,
                        "Không tìm thấy tồn kho tại site " + site
                                + " khi áp dụng Global COMMIT: transactionId=" + transactionId
                                + ", warehouseId=" + inventoryId.getWarehouseId()
                                + ", productId=" + inventoryId.getProductId() + "."));

        if (getReservedQuantity(inventory) < quantity) {
            throw new AppException(ErrorCode.INCONSISTENT_RESERVED_STOCK,
                    "Không thể áp dụng Global COMMIT vì reserved_quantity không đủ tại site " + site
                            + ": transactionId=" + transactionId
                            + ", warehouseId=" + inventoryId.getWarehouseId()
                            + ", productId=" + inventoryId.getProductId()
                            + ", số lượng cần commit=" + quantity
                            + ", reserved_quantity hiện tại=" + getReservedQuantity(inventory) + ".");
        }

        inventory.setReservedQuantity(getReservedQuantity(inventory) - quantity);
        inventoryRepository.save(inventory);

        saveParticipantLog(site, logRepository, transactionId, inventoryId, quantity,
                TransactionStatus.COMMITTED, "Đã áp dụng Global COMMIT");
        log.info("{} 2PC COMMIT thành công, transactionId={}, id={}", site, transactionId, inventoryId);
    }

    private void abortReservationWithLock(
            String site,
            InventoryLockingRepository inventoryRepository,
            JpaRepository<TransactionParticipantLog, Integer> logRepository,
            String transactionId,
            InventoryId inventoryId,
            Integer quantity) {

        log.info("{} cần khóa cho 2PC ABORT với id: {}, transactionId={}", site, inventoryId, transactionId);

        // ABORT hoàn tác reservation: trả reserved_quantity về quantity khả dụng.
        Inventory inventory = inventoryRepository.findByIdForUpdate(inventoryId)
                .orElseThrow(() -> new AppException(ErrorCode.INSUFFICIENT_STOCK,
                        "Không tìm thấy tồn kho tại site " + site
                                + " khi áp dụng Global ABORT: transactionId=" + transactionId
                                + ", warehouseId=" + inventoryId.getWarehouseId()
                                + ", productId=" + inventoryId.getProductId() + "."));

        if (getReservedQuantity(inventory) < quantity) {
            throw new AppException(ErrorCode.INCONSISTENT_RESERVED_STOCK,
                    "Không thể áp dụng Global ABORT vì reserved_quantity không đủ tại site " + site
                            + ": transactionId=" + transactionId
                            + ", warehouseId=" + inventoryId.getWarehouseId()
                            + ", productId=" + inventoryId.getProductId()
                            + ", số lượng cần hoàn tác=" + quantity
                            + ", reserved_quantity hiện tại=" + getReservedQuantity(inventory) + ".");
        }

        inventory.setReservedQuantity(getReservedQuantity(inventory) - quantity);
        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventoryRepository.save(inventory);

        saveParticipantLog(site, logRepository, transactionId, inventoryId, quantity,
                TransactionStatus.ABORTED, "Đã áp dụng Global ABORT");
        log.info("{} 2PC ABORT thành công, transactionId={}, id={}", site, transactionId, inventoryId);
    }

    private void saveParticipantLog(
            String site,
            JpaRepository<TransactionParticipantLog, Integer> logRepository,
            String transactionId,
            InventoryId inventoryId,
            Integer quantity,
            TransactionStatus status,
            String message) {
        logRepository.save(TransactionParticipantLog.builder()
                .transactionId(transactionId)
                .siteCode(site)
                .warehouseId(inventoryId.getWarehouseId())
                .productId(inventoryId.getProductId())
                .quantity(quantity)
                .status(status)
                .message(message)
                .build());
    }

    private Integer getReservedQuantity(Inventory inventory) {
        return inventory.getReservedQuantity() == null ? 0 : inventory.getReservedQuantity();
    }
}
