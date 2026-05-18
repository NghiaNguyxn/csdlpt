package com.example.csdlpt.controller;

import com.example.csdlpt.dto.request.FallbackOrderRequest;
import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.FallbackOrderResponse;
import com.example.csdlpt.service.FallbackOrderService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FallbackOrderController {

    FallbackOrderService fallbackOrderService;

    @PostMapping("/fallback")
    public ApiResponse<FallbackOrderResponse> createFallbackOrder(@RequestBody FallbackOrderRequest request) {
        return ApiResponse.ok(fallbackOrderService.createFallbackOrder(request));
    }
}
