package com.example.csdlpt.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.GlobalStockBenchmarkResponse;
import com.example.csdlpt.service.BenchmarkService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/benchmarks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BenchmarkController {

    BenchmarkService benchmarkService;

    @GetMapping("/global-stock")
    public ApiResponse<GlobalStockBenchmarkResponse> benchmarkGlobalStock(
            @RequestParam Integer productId,
            @RequestParam(required = false) Integer iterations,
            @RequestParam(required = false) Integer warmup) {
        return ApiResponse.ok(benchmarkService.benchmarkGlobalStock(productId, iterations, warmup));
    }
}
