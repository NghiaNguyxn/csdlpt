package com.example.csdlpt.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.csdlpt.dto.request.DistributedOrderRequest;
import com.example.csdlpt.dto.request.LocalOrderRequest;
import com.example.csdlpt.dto.request.OrderStatusRequest;
import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.DistributedOrderResponse;
import com.example.csdlpt.dto.response.OrderResponse;
import com.example.csdlpt.service.Order.OrderService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderController {

    OrderService orderService;

    @PostMapping("/distributed")
    public ApiResponse<DistributedOrderResponse> placeDistributedOrder(
            @RequestBody DistributedOrderRequest request) {
        return ApiResponse.ok(orderService.placeDistributedOrder(request));
    }

    @PostMapping("/local")
    public ApiResponse<OrderResponse> createLocalOrder(@RequestBody LocalOrderRequest request) {
        return ApiResponse.ok(orderService.createLocalOrder(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable Long id) {
        return ApiResponse.ok(orderService.getOrderById(id));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getAllOrders() {
        return ApiResponse.ok(orderService.getAllOrders());
    }

    @GetMapping("/site/{siteCode}")
    public ApiResponse<List<OrderResponse>> getOrdersBySite(@PathVariable String siteCode) {
        return ApiResponse.ok(orderService.getAllOrdersBySite(siteCode));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable Long id, @RequestBody OrderStatusRequest request) {
        return ApiResponse.ok(orderService.updateStatus(id, request));
    }
}
