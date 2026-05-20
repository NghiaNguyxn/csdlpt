package com.example.csdlpt.service;

import com.example.csdlpt.dto.request.OrderAllocationRequest;
import com.example.csdlpt.entity.TransactionParticipantLog;
import com.example.csdlpt.enums.SiteCode;
import com.example.csdlpt.enums.TransactionStatus;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.common.DistributedInventoryRepository;
import com.example.csdlpt.repository.common.DistributedTransactionParticipantLogRepository;
import com.example.csdlpt.repository.site_dn.DanangInventoryRepository;
import com.example.csdlpt.repository.site_dn.DanangTransactionParticipantLogRepository;
import com.example.csdlpt.repository.site_hcm.HcmInventoryRepository;
import com.example.csdlpt.repository.site_hcm.HcmTransactionParticipantLogRepository;
import com.example.csdlpt.repository.site_hn.HanoiInventoryRepository;
import com.example.csdlpt.repository.site_hn.HanoiTransactionParticipantLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TwoPhaseCommitStockService {
    private final HanoiInventoryRepository hanoiInventoryRepository;
    private final DanangInventoryRepository danangInventoryRepository;
    private final HcmInventoryRepository hcmInventoryRepository;

    private final HanoiTransactionParticipantLogRepository hanoiParticipantLogRepository;
    private final DanangTransactionParticipantLogRepository danangParticipantLogRepository;
    private final HcmTransactionParticipantLogRepository hcmParticipantLogRepository;

    @Transactional(value = "hanoiTransactionManager", noRollbackFor = AppException.class)
    public void prepareAtHanoi(String txId, Integer warehouseId, Integer productId, Integer quantity) {
        prepare(SiteCode.HN, hanoiInventoryRepository, hanoiParticipantLogRepository, txId, warehouseId, productId, quantity);
    }

    @Transactional(value = "danangTransactionManager", noRollbackFor = AppException.class)
    public void prepareAtDanang(String txId, Integer warehouseId, Integer productId, Integer quantity) {
        prepare(SiteCode.DN, danangInventoryRepository, danangParticipantLogRepository, txId, warehouseId, productId, quantity);
    }

    @Transactional(value = "hcmTransactionManager", noRollbackFor = AppException.class)
    public void prepareAtHcm(String txId, Integer warehouseId, Integer productId, Integer quantity) {
        prepare(SiteCode.HCM, hcmInventoryRepository, hcmParticipantLogRepository, txId, warehouseId, productId, quantity);
    }

    @Transactional("hanoiTransactionManager")
    public void commitAtHanoi(String txId, Integer warehouseId, Integer productId, Integer quantity) {
        commit(SiteCode.HN, hanoiInventoryRepository, hanoiParticipantLogRepository, txId, warehouseId, productId, quantity);
    }

    @Transactional("danangTransactionManager")
    public void commitAtDanang(String txId, Integer warehouseId, Integer productId, Integer quantity) {
        commit(SiteCode.DN, danangInventoryRepository, danangParticipantLogRepository, txId, warehouseId, productId, quantity);
    }

    @Transactional("hcmTransactionManager")
    public void commitAtHcm(String txId, Integer warehouseId, Integer productId, Integer quantity) {
        commit(SiteCode.HCM, hcmInventoryRepository, hcmParticipantLogRepository, txId, warehouseId, productId, quantity);
    }

    @Transactional("hanoiTransactionManager")
    public void abortAtHanoi(String txId, Integer warehouseId, Integer productId, Integer quantity) {
        abort(SiteCode.HN, hanoiInventoryRepository, hanoiParticipantLogRepository, txId, warehouseId, productId, quantity);
    }

    @Transactional("danangTransactionManager")
    public void abortAtDanang(String txId, Integer warehouseId, Integer productId, Integer quantity) {
        abort(SiteCode.DN, danangInventoryRepository, danangParticipantLogRepository, txId, warehouseId, productId, quantity);
    }

    @Transactional("hcmTransactionManager")
    public void abortAtHcm(String txId, Integer warehouseId, Integer productId, Integer quantity) {
        abort(SiteCode.HCM, hcmInventoryRepository, hcmParticipantLogRepository, txId, warehouseId, productId, quantity);
    }

    public void prepare(SiteCode site, String txId, OrderAllocationRequest allocation, Integer productId) {
        switch (site) {
            case HN -> prepareAtHanoi(txId, allocation.getWarehouseId(), productId, allocation.getQuantity());
            case DN -> prepareAtDanang(txId, allocation.getWarehouseId(), productId, allocation.getQuantity());
            case HCM -> prepareAtHcm(txId, allocation.getWarehouseId(), productId, allocation.getQuantity());
        }
    }

    public void commit(SiteCode site, String txId, OrderAllocationRequest allocation, Integer productId) {
        switch (site) {
            case HN -> commitAtHanoi(txId, allocation.getWarehouseId(), productId, allocation.getQuantity());
            case DN -> commitAtDanang(txId, allocation.getWarehouseId(), productId, allocation.getQuantity());
            case HCM -> commitAtHcm(txId, allocation.getWarehouseId(), productId, allocation.getQuantity());
        }
    }

    public void abort(SiteCode site, String txId, OrderAllocationRequest allocation, Integer productId) {
        switch (site) {
            case HN -> abortAtHanoi(txId, allocation.getWarehouseId(), productId, allocation.getQuantity());
            case DN -> abortAtDanang(txId, allocation.getWarehouseId(), productId, allocation.getQuantity());
            case HCM -> abortAtHcm(txId, allocation.getWarehouseId(), productId, allocation.getQuantity());
        }
    }

    private void prepare(SiteCode site,
                         DistributedInventoryRepository inventoryRepository,
                         DistributedTransactionParticipantLogRepository logRepository,
                         String txId,
                         Integer warehouseId,
                         Integer productId,
                         Integer quantity) {
        int updated = inventoryRepository.prepareStock(warehouseId, productId, quantity);
        if (updated == 0) {
            saveLog(logRepository, txId, site, warehouseId, productId, quantity,
                    TransactionStatus.ABORTED,
                    "Vote NO: không đủ tồn kho khả dụng trong pha PREPARE");
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                    "PREPARE thất bại tại site " + site + ": không đủ tồn kho khả dụng");
        }
        saveLog(logRepository, txId, site, warehouseId, productId, quantity,
                TransactionStatus.PREPARED,
                "Vote YES: đã giữ tạm tồn kho bằng reserved_quantity");
    }

    private void commit(SiteCode site,
                        DistributedInventoryRepository inventoryRepository,
                        DistributedTransactionParticipantLogRepository logRepository,
                        String txId,
                        Integer warehouseId,
                        Integer productId,
                        Integer quantity) {
        int updated = inventoryRepository.commitPreparedStock(warehouseId, productId, quantity);
        if (updated == 0) {
            throw new AppException(ErrorCode.UPDATE_FAILED,
                    "COMMIT thất bại tại site " + site + ": reserved_quantity không đủ");
        }
        saveLog(logRepository, txId, site, warehouseId, productId, quantity,
                TransactionStatus.COMMITTED,
                "Đã nhận Global COMMIT và trừ tồn kho chính thức");
    }

    private void abort(SiteCode site,
                       DistributedInventoryRepository inventoryRepository,
                       DistributedTransactionParticipantLogRepository logRepository,
                       String txId,
                       Integer warehouseId,
                       Integer productId,
                       Integer quantity) {
        int updated = inventoryRepository.abortPreparedStock(warehouseId, productId, quantity);
        String message = updated == 0
                ? "Global ABORT: không còn reserved_quantity cần giải phóng"
                : "Đã nhận Global ABORT và giải phóng reserved_quantity";
        saveLog(logRepository, txId, site, warehouseId, productId, quantity,
                TransactionStatus.ABORTED,
                message);
    }

    private void saveLog(DistributedTransactionParticipantLogRepository logRepository,
                         String txId,
                         SiteCode site,
                         Integer warehouseId,
                         Integer productId,
                         Integer quantity,
                         TransactionStatus status,
                         String message) {
        logRepository.save(TransactionParticipantLog.builder()
                .transactionId(txId)
                .siteCode(site.name())
                .warehouseId(warehouseId)
                .productId(productId)
                .quantity(quantity)
                .status(status)
                .message(message)
                .build());
    }
}
