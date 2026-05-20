package com.example.csdlpt.repository.site_dn;

import com.example.csdlpt.repository.common.DistributedInventoryRepository;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.repository.InventoryLockingRepository;

@Repository
public interface DanangInventoryRepository extends DistributedInventoryRepository, InventoryLockingRepository {
    List<Inventory> findByProductId(Integer productId);
}
