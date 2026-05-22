package com.example.csdlpt.repository.site_hn;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.repository.InventoryLockingRepository;

@Repository
public interface HanoiInventoryRepository extends InventoryLockingRepository {
    List<Inventory> findByProductId(Integer productId);
}
