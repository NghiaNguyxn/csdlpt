package com.example.csdlpt.repository.site_hcm;

import com.example.csdlpt.entity.OrderDetail;
import com.example.csdlpt.entity.OrderDetailId;
import com.example.csdlpt.repository.MultiWarehouseOrderLineProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HcmOrderDetailRepository extends JpaRepository<OrderDetail, OrderDetailId> {

    @Query(value = """
            SELECT od.order_id AS orderId,
                   od.product_id AS productId,
                   pb.name AS productName,
                   od.warehouse_id AS warehouseId,
                   w.code AS warehouseCode,
                   w.name AS warehouseName,
                   s.site_code AS siteCode,
                   od.quantity AS quantity,
                   od.price AS price
            FROM order_detail od
            JOIN product_basic pb ON pb.id = od.product_id
            JOIN warehouse w ON w.id = od.warehouse_id
            JOIN site s ON s.id = w.site_id
            ORDER BY od.order_id, od.warehouse_id, od.product_id
            """, nativeQuery = true)
    List<MultiWarehouseOrderLineProjection> findDistributedOrderLines();
}
