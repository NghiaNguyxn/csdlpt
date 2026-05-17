package com.example.csdlpt.repository.site_dn;

import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.entity.InventoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DanangInventoryRepository extends JpaRepository<Inventory, InventoryId> {
    @Query(value = "SELECT * FROM inventory WHERE product_id = :productId", nativeQuery = true)
    List<Inventory> findByProductId(@Param("productId") Integer productId);

    @Query(value = "SELECT COALESCE(SUM(quantity), 0) FROM inventory WHERE product_id = :productId", nativeQuery = true)
    Integer sumQuantityByProductId(@Param("productId") Integer productId);
}
