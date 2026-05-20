package com.example.csdlpt.controller;

import com.example.csdlpt.dto.request.LocalOrderRequest;
import com.example.csdlpt.dto.request.MultiOrderRequest;
import com.example.csdlpt.dto.request.OrderStatusRequest;
import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.OrderResponse;
import com.example.csdlpt.service.OrderManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderManagementController {
    private final OrderManagementService orderService;

    @PostMapping("/local")
    public ApiResponse<OrderResponse> createLocalOrder(@RequestBody LocalOrderRequest request) {
        return ApiResponse.ok(orderService.createLocalHanoiOrder(request));
    }

    @PostMapping("/multi")
    public ApiResponse<OrderResponse> createMultiOrder(@RequestBody MultiOrderRequest request) {
        return ApiResponse.ok(orderService.createMultiWarehouseOrder(request));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getOrders() {
        return ApiResponse.ok(orderService.getOrders());
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable Long id) {
        return ApiResponse.ok(orderService.getOrderById(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable Long id, @RequestBody OrderStatusRequest request) {
        return ApiResponse.ok(orderService.updateStatus(id, request));
    }
}
