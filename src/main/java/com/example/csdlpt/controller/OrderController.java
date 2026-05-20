package com.example.csdlpt.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.csdlpt.dto.request.DistributedOrderRequest;
import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.DistributedOrderResponse;
import com.example.csdlpt.service.OrderService;

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
}
