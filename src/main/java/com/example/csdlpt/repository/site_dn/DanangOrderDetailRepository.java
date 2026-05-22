package com.example.csdlpt.repository.site_dn;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.csdlpt.entity.OrderDetail;
import com.example.csdlpt.entity.OrderDetailId;
import com.example.csdlpt.repository.MultiWarehouseOrderLineProjection;

@Repository
public interface DanangOrderDetailRepository extends JpaRepository<OrderDetail, OrderDetailId> {

    /**
     * Q5-A: Đơn hàng lấy từ nhiều kho khác nhau (HAVING COUNT(DISTINCT warehouse_id) > 1).
     * CTE lọc qualifying orders tại DB — chỉ kéo về các dòng detail của đơn đủ điều kiện.
     */
    @Query(value = """
            WITH qualifying_orders AS (
                SELECT od.order_id
                FROM order_detail od
                JOIN warehouse w ON w.id = od.warehouse_id
                GROUP BY od.order_id
                HAVING COUNT(DISTINCT od.warehouse_id) > 1
            )
            SELECT od.order_id   AS orderId,
                   od.product_id AS productId,
                   pb.name       AS productName,
                   od.warehouse_id AS warehouseId,
                   w.code        AS warehouseCode,
                   w.name        AS warehouseName,
                   s.site_code   AS siteCode,
                   od.quantity   AS quantity,
                   od.price      AS price
            FROM order_detail od
            JOIN qualifying_orders qo ON qo.order_id = od.order_id
            JOIN product_basic pb ON pb.id = od.product_id
            JOIN warehouse w      ON w.id  = od.warehouse_id
            JOIN site s           ON s.id  = w.site_id
            ORDER BY od.order_id, od.warehouse_id, od.product_id
            """, nativeQuery = true)
    List<MultiWarehouseOrderLineProjection> findMultiWarehouseOrderLines();

    /**
     * Q5-B: Đơn hàng lấy từ nhiều site khác nhau (HAVING COUNT(DISTINCT site_id) > 1).
     * CTE lọc qualifying orders tại DB.
     */
    @Query(value = """
            WITH qualifying_orders AS (
                SELECT od.order_id
                FROM order_detail od
                JOIN warehouse w ON w.id = od.warehouse_id
                GROUP BY od.order_id
                HAVING COUNT(DISTINCT w.site_id) > 1
            )
            SELECT od.order_id   AS orderId,
                   od.product_id AS productId,
                   pb.name       AS productName,
                   od.warehouse_id AS warehouseId,
                   w.code        AS warehouseCode,
                   w.name        AS warehouseName,
                   s.site_code   AS siteCode,
                   od.quantity   AS quantity,
                   od.price      AS price
            FROM order_detail od
            JOIN qualifying_orders qo ON qo.order_id = od.order_id
            JOIN product_basic pb ON pb.id = od.product_id
            JOIN warehouse w      ON w.id  = od.warehouse_id
            JOIN site s           ON s.id  = w.site_id
            ORDER BY od.order_id, od.warehouse_id, od.product_id
            """, nativeQuery = true)
    List<MultiWarehouseOrderLineProjection> findMultiSiteOrderLines();
}
