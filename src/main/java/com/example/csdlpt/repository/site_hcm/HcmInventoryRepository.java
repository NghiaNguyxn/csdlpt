package com.example.csdlpt.repository.site_hcm;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.repository.InventoryLockingRepository;

@Repository
public interface HcmInventoryRepository extends InventoryLockingRepository {
    List<Inventory> findByProductId(Integer productId);
}
