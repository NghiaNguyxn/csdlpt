package com.example.csdlpt.controller;

import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.MultiWarehouseOrderResponse;
import com.example.csdlpt.dto.response.QueryPerformanceResponse;
import com.example.csdlpt.service.DistributedQueryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/distributed-queries")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DistributedQueryController {

    DistributedQueryService distributedQueryService;

    @GetMapping("/q5/orders/multi-warehouse")
    public ApiResponse<List<MultiWarehouseOrderResponse>> findOrdersExportedFromMultipleWarehouses() {
        return ApiResponse.ok(distributedQueryService.findOrdersExportedFromMultipleWarehouses());
    }

    @GetMapping("/q6/inventory/{productId}/performance")
    public ApiResponse<QueryPerformanceResponse> compareQ1CentralizedAndDistributed(@PathVariable Integer productId) {
        return ApiResponse.ok(distributedQueryService.compareQ1CentralizedAndDistributed(productId));
    }

}
