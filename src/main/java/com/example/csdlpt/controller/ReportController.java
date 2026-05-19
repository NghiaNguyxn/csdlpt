package com.example.csdlpt.controller;

import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.MonthlyRevenueResponse;
import com.example.csdlpt.dto.response.RevenueSummaryResponse;
import com.example.csdlpt.dto.response.TopSellingResponse;
import com.example.csdlpt.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

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
    public ApiResponse<List<MonthlyRevenueResponse>> getRevenueBySite(
            @RequestParam(required = false) Integer year) {
        Integer queryYear = year == null ? LocalDate.now().getYear() : year;
        return ApiResponse.ok(reportService.getMonthlyRevenue(queryYear).getDetails());
    }

    @GetMapping("/revenue/total")
    public ApiResponse<RevenueSummaryResponse> getTotalRevenue(@RequestParam(required = false) Integer year) {
        Integer queryYear = year == null ? LocalDate.now().getYear() : year;
        return ApiResponse.ok(reportService.getMonthlyRevenue(queryYear));
    }

    @GetMapping("/top-selling")
    public ApiResponse<List<TopSellingResponse>> getTopSelling(@RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(reportService.getTopSelling(limit));
    }
}
