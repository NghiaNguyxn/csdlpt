package com.example.csdlpt.controller;

import java.util.List;

import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.QueryPerformanceResponse;
import com.example.csdlpt.dto.response.TransactionEventLogResponse;
import com.example.csdlpt.service.DistributedQueryService;
import com.example.csdlpt.service.TransactionDemoLogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sys")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemController {

    DistributedQueryService distributedQueryService;
    TransactionDemoLogService transactionDemoLogService;

    @GetMapping("/benchmark")
    public ApiResponse<QueryPerformanceResponse> benchmark(
            @RequestParam(defaultValue = "1") Integer productId) {
        return ApiResponse.ok(distributedQueryService.compareQ1CentralizedAndDistributed(productId));
    }

    @GetMapping("/transaction-logs")
    public ApiResponse<List<TransactionEventLogResponse>> findRecentTransactionEvents() {
        return ApiResponse.ok(transactionDemoLogService.findRecentEvents());
    }

    @GetMapping("/transaction-logs/{transactionId}")
    public ApiResponse<List<TransactionEventLogResponse>> findTransactionEvents(@PathVariable String transactionId) {
        return ApiResponse.ok(transactionDemoLogService.findEventsByTransactionId(transactionId));
    }
}
