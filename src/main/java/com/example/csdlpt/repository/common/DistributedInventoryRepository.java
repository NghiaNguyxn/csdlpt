package com.example.csdlpt.repository.common;

import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.entity.InventoryId;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@NoRepositoryBean
public interface DistributedInventoryRepository extends JpaRepository<Inventory, InventoryId> {
    List<Inventory> findByProductId(Integer productId);

    @Query(value = "SELECT COUNT(*) > 0 FROM inventory WHERE warehouse_id = :warehouseId", nativeQuery = true)
    boolean existsByWarehouseId(@Param("warehouseId") Integer warehouseId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE inventory SET quantity = quantity - :quantity " +
            "WHERE warehouse_id = :warehouseId AND product_id = :productId " +
            "AND quantity - reserved_quantity >= :quantity", nativeQuery = true)
    int reduceStockIfEnough(@Param("warehouseId") Integer warehouseId,
                            @Param("productId") Integer productId,
                            @Param("quantity") Integer quantity);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE inventory SET quantity = quantity + :quantity " +
            "WHERE warehouse_id = :warehouseId AND product_id = :productId", nativeQuery = true)
    int addStockBack(@Param("warehouseId") Integer warehouseId,
                     @Param("productId") Integer productId,
                     @Param("quantity") Integer quantity);

    @Query(value = "SELECT quantity FROM inventory WHERE warehouse_id = :warehouseId AND product_id = :productId", nativeQuery = true)
    Integer findQuantityNative(@Param("warehouseId") Integer warehouseId,
                               @Param("productId") Integer productId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE inventory
            SET reserved_quantity = reserved_quantity + :quantity
            WHERE warehouse_id = :warehouseId
              AND product_id = :productId
              AND quantity - reserved_quantity >= :quantity
            """, nativeQuery = true)
    int prepareStock(@Param("warehouseId") Integer warehouseId,
                     @Param("productId") Integer productId,
                     @Param("quantity") Integer quantity);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE inventory
            SET quantity = quantity - :quantity,
                reserved_quantity = reserved_quantity - :quantity
            WHERE warehouse_id = :warehouseId
              AND product_id = :productId
              AND reserved_quantity >= :quantity
            """, nativeQuery = true)
    int commitPreparedStock(@Param("warehouseId") Integer warehouseId,
                            @Param("productId") Integer productId,
                            @Param("quantity") Integer quantity);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE inventory
            SET reserved_quantity = reserved_quantity - :quantity
            WHERE warehouse_id = :warehouseId
              AND product_id = :productId
              AND reserved_quantity >= :quantity
            """, nativeQuery = true)
    int abortPreparedStock(@Param("warehouseId") Integer warehouseId,
                           @Param("productId") Integer productId,
                           @Param("quantity") Integer quantity);

    @Query(value = """
            SELECT COALESCE(quantity - reserved_quantity, 0)
            FROM inventory
            WHERE warehouse_id = :warehouseId AND product_id = :productId
            """, nativeQuery = true)
    Integer findAvailableQuantityNative(@Param("warehouseId") Integer warehouseId,
                                        @Param("productId") Integer productId);

}