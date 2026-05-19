package com.example.csdlpt.repository.common;

import com.example.csdlpt.entity.Order;
import com.example.csdlpt.enums.OrderStatus;
import com.example.csdlpt.repository.projection.MonthlyRevenueProjection;
import com.example.csdlpt.repository.projection.TopSellingProjection;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@NoRepositoryBean
public interface DistributedOrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(OrderStatus status);

    @Query(value = "SELECT CAST(EXTRACT(MONTH FROM o.order_date) AS INTEGER) AS month, " +
            "od.warehouse_id AS warehouseId, SUM(od.quantity * od.price) AS revenue " +
            "FROM orders o JOIN order_detail od ON o.id = od.order_id " +
            "WHERE o.status = 'COMPLETED' AND CAST(EXTRACT(YEAR FROM o.order_date) AS INTEGER) = :year " +
            "GROUP BY month, od.warehouse_id ORDER BY month, od.warehouse_id", nativeQuery = true)
    List<MonthlyRevenueProjection> getMonthlyRevenue(@Param("year") Integer year);

    @Query(value = "SELECT od.product_id AS productId, CAST(SUM(od.quantity) AS BIGINT) AS totalSold " +
            "FROM orders o JOIN order_detail od ON o.id = od.order_id " +
            "WHERE o.status = 'COMPLETED' " +
            "GROUP BY od.product_id", nativeQuery = true)
    List<TopSellingProjection> getTopSellingLocal();
}
