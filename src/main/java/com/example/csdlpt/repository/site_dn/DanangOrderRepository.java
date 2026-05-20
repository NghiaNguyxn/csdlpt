package com.example.csdlpt.repository.site_dn;

import com.example.csdlpt.repository.common.DistributedOrderRepository;
import com.example.csdlpt.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DanangOrderRepository extends DistributedOrderRepository {
}
