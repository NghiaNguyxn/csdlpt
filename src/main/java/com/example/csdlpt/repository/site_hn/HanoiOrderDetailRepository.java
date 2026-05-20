package com.example.csdlpt.repository.site_hn;

import com.example.csdlpt.repository.common.DistributedOrderDetailRepository;
import com.example.csdlpt.entity.OrderDetail;
import com.example.csdlpt.entity.OrderDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HanoiOrderDetailRepository extends DistributedOrderDetailRepository {
}
