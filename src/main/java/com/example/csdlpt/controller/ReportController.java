package com.example.csdlpt.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.RevenueSummaryResponse;
import com.example.csdlpt.dto.response.SiteRevenueResponse;
import com.example.csdlpt.dto.response.TopSellingResponse;
import com.example.csdlpt.dto.response.TotalRevenueResponse;
import com.example.csdlpt.dto.response.WarehouseRevenueResponse;
import com.example.csdlpt.service.ReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/revenue/monthly")
    public ApiResponse<RevenueSummaryResponse> getMonthlyRevenue(@RequestParam(required = false) Integer year) {
        Integer queryYear = year == null ? LocalDate.now().getYear() : year;
        return ApiResponse.ok(reportService.getMonthlyRevenue(queryYear));
    }

    @GetMapping("/revenue/site")
    public ApiResponse<List<SiteRevenueResponse>> getRevenueBySite(@RequestParam(required = false) Integer year) {
        Integer queryYear = year == null ? LocalDate.now().getYear() : year;
        return ApiResponse.ok(reportService.getRevenueBySite(queryYear));
    }

    @GetMapping("/revenue/warehouse")
    public ApiResponse<List<WarehouseRevenueResponse>> getRevenueByWarehouse(@RequestParam(required = false) Integer year) {
        Integer queryYear = year == null ? LocalDate.now().getYear() : year;
        return ApiResponse.ok(reportService.getRevenueByWarehouse(queryYear));
    }

    @GetMapping("/revenue/total")
    public ApiResponse<TotalRevenueResponse> getTotalRevenue(@RequestParam(required = false) Integer year) {
        Integer queryYear = year == null ? LocalDate.now().getYear() : year;
        return ApiResponse.ok(reportService.getTotalRevenue(queryYear));
    }

    @GetMapping("/top-selling")
    public ApiResponse<List<TopSellingResponse>> getTopSelling(@RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(reportService.getTopSelling(limit));
    }
}
