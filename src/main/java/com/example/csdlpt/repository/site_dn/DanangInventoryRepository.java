package com.example.csdlpt.repository.site_dn;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.repository.InventoryLockingRepository;

@Repository
public interface DanangInventoryRepository extends InventoryLockingRepository {
    List<Inventory> findByProductId(Integer productId);

    @Query(value = "SELECT COALESCE(SUM(quantity), 0) FROM inventory WHERE product_id = :productId", nativeQuery = true)
    Integer sumQuantityByProductId(@Param("productId") Integer productId);
}
