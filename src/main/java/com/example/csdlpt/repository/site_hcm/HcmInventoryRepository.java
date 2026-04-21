package com.example.csdlpt.repository.site_hcm;

import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.entity.InventoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HcmInventoryRepository extends JpaRepository<Inventory, InventoryId> {
    List<Inventory> findByProductId(Integer productId);
}
