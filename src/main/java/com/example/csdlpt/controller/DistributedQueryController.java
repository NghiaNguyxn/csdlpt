package com.example.csdlpt.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.MultiWarehouseOrderResponse;
import com.example.csdlpt.service.DistributedQueryService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Q5 — Distributed Join: đơn hàng xuất từ nhiều kho/site.
 *
 * Lưu ý kiến trúc: order và toàn bộ order_detail được lưu tại site coordinator
 * tạo đơn (horizontal fragmentation theo site). Warehouse + Site được replicate.
 *
 * Pushdown strategy: mỗi site chạy CTE GROUP BY / HAVING tại DB để lọc qualifying
 * order_id, chỉ trả về detail lines của đơn đủ điều kiện. Coordinator union 3 site.
 */
@RestController
@RequestMapping("/api/distributed-queries")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DistributedQueryController {

    DistributedQueryService distributedQueryService;

    /**
     * Q5-A: đơn hàng lấy hàng từ nhiều KHO khác nhau.
     * DB filter: HAVING COUNT(DISTINCT warehouse_id) > 1
     * GET /api/distributed-queries/q5/orders/multi-warehouse
     */
    @GetMapping("/q5/orders/multi-warehouse")
    public ApiResponse<List<MultiWarehouseOrderResponse>> findOrdersFromMultipleWarehouses() {
        return ApiResponse.ok(distributedQueryService.findOrdersFromMultipleWarehouses());
    }

    /**
     * Q5-B: đơn hàng lấy hàng từ nhiều SITE khác nhau.
     * DB filter: HAVING COUNT(DISTINCT site_id) > 1
     * GET /api/distributed-queries/q5/orders/multi-site
     */
    @GetMapping("/q5/orders/multi-site")
    public ApiResponse<List<MultiWarehouseOrderResponse>> findOrdersFromMultipleSites() {
        return ApiResponse.ok(distributedQueryService.findOrdersFromMultipleSites());
    }
}
