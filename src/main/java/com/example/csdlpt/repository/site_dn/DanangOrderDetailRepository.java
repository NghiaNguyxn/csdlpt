package com.example.csdlpt.repository.site_dn;

import com.example.csdlpt.entity.OrderDetail;
import com.example.csdlpt.entity.OrderDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DanangOrderDetailRepository extends JpaRepository<OrderDetail, OrderDetailId> {
}
