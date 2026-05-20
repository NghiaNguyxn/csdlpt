package com.example.csdlpt.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.csdlpt.entity.Order;
import com.example.csdlpt.entity.OrderDetail;
import com.example.csdlpt.entity.TransactionLog;
import com.example.csdlpt.enums.OrderStatus;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.site_dn.DanangOrderDetailRepository;
import com.example.csdlpt.repository.site_dn.DanangOrderRepository;
import com.example.csdlpt.repository.site_dn.DanangTransactionLogRepository;
import com.example.csdlpt.repository.site_hcm.HcmOrderDetailRepository;
import com.example.csdlpt.repository.site_hcm.HcmOrderRepository;
import com.example.csdlpt.repository.site_hcm.HcmTransactionLogRepository;
import com.example.csdlpt.repository.site_hn.HanoiOrderDetailRepository;
import com.example.csdlpt.repository.site_hn.HanoiOrderRepository;
import com.example.csdlpt.repository.site_hn.HanoiTransactionLogRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderPersistenceService {

    HanoiOrderRepository hanoiOrderRepository;
    HanoiOrderDetailRepository hanoiOrderDetailRepository;
    HanoiTransactionLogRepository hanoiTransactionLogRepository;

    DanangOrderRepository danangOrderRepository;
    DanangOrderDetailRepository danangOrderDetailRepository;
    DanangTransactionLogRepository danangTransactionLogRepository;

    HcmOrderRepository hcmOrderRepository;
    HcmOrderDetailRepository hcmOrderDetailRepository;
    HcmTransactionLogRepository hcmTransactionLogRepository;

    @Transactional("hanoiTransactionManager")
    public void saveTransactionLogAtHanoi(TransactionLog transactionLog) {
        hanoiTransactionLogRepository.save(transactionLog);
    }

    @Transactional("danangTransactionManager")
    public void saveTransactionLogAtDanang(TransactionLog transactionLog) {
        danangTransactionLogRepository.save(transactionLog);
    }

    @Transactional("hcmTransactionManager")
    public void saveTransactionLogAtHcm(TransactionLog transactionLog) {
        hcmTransactionLogRepository.save(transactionLog);
    }

    @Transactional("hanoiTransactionManager")
    public Order createOrderAtHanoi(Order order, List<OrderDetail> details) {
        Order savedOrder = hanoiOrderRepository.save(order);
        hanoiOrderDetailRepository.saveAll(details);
        return savedOrder;
    }

    @Transactional("danangTransactionManager")
    public Order createOrderAtDanang(Order order, List<OrderDetail> details) {
        Order savedOrder = danangOrderRepository.save(order);
        danangOrderDetailRepository.saveAll(details);
        return savedOrder;
    }

    @Transactional("hcmTransactionManager")
    public Order createOrderAtHcm(Order order, List<OrderDetail> details) {
        Order savedOrder = hcmOrderRepository.save(order);
        hcmOrderDetailRepository.saveAll(details);
        return savedOrder;
    }

    @Transactional("hanoiTransactionManager")
    public void updateOrderStatusAtHanoi(Long orderId, OrderStatus status) {
        Order order = hanoiOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng tại HN"));
        order.setStatus(status);
        hanoiOrderRepository.save(order);
    }

    @Transactional("danangTransactionManager")
    public void updateOrderStatusAtDanang(Long orderId, OrderStatus status) {
        Order order = danangOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng tại DN"));
        order.setStatus(status);
        danangOrderRepository.save(order);
    }

    @Transactional("hcmTransactionManager")
    public void updateOrderStatusAtHcm(Long orderId, OrderStatus status) {
        Order order = hcmOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng tại HCM"));
        order.setStatus(status);
        hcmOrderRepository.save(order);
    }
}
