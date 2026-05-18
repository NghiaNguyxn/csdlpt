package com.example.csdlpt.service;

import com.example.csdlpt.entity.Order;
import com.example.csdlpt.entity.OrderDetail;
import com.example.csdlpt.entity.TransactionLog;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
