package com.example.csdlpt.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.csdlpt.dto.request.InventoryRequest;
import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.AvailableSiteResponse;
import com.example.csdlpt.dto.response.InventoryResponse;
import com.example.csdlpt.dto.response.StockResponse;
import com.example.csdlpt.enums.SiteCode;
import com.example.csdlpt.service.InventoryService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryController {

    InventoryService inventoryService;

    @GetMapping("/{productId}/local")
    public ApiResponse<StockResponse> getStockBySite(
            @PathVariable Integer productId,
            @RequestParam(required = false) SiteCode siteCode) {

        
        
        return ApiResponse.ok(inventoryService.getStockBySite(productId, siteCode));

    }

    @GetMapping("/{productId}/global")
    public ApiResponse<StockResponse> getGlobalStock(@PathVariable Integer productId) {

        return ApiResponse.ok(inventoryService.getGlobalStock(productId));

    }

    @GetMapping("/available")
    public ApiResponse<List<AvailableSiteResponse>> findSiteWithEnoughStock(
            @RequestParam Integer productId,
            @RequestParam Integer quantity) {

        return ApiResponse.ok(inventoryService.findSiteWithEnoughStock(productId, quantity));

    }

    @PostMapping("/add")
    public ApiResponse<InventoryResponse> addStock(@RequestBody InventoryRequest request) {

        return ApiResponse.ok(inventoryService.addStock(request));

    }

    @PostMapping("/reduce")
    public ApiResponse<InventoryResponse> reduceStock(@RequestBody InventoryRequest request) {

        return ApiResponse.ok(inventoryService.reduceStock(request));

    }

}
