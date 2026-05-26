package com.example.csdlpt.repository.site_hcm;

import java.util.List;

import com.example.csdlpt.entity.Order;
import com.example.csdlpt.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HcmOrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(OrderStatus status);
}
