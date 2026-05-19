package com.example.csdlpt.controller;

import com.example.csdlpt.dto.request.WarehouseRequest;
import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.WarehouseResponse;
import com.example.csdlpt.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {
    private final WarehouseService warehouseService;

    @GetMapping
    public ApiResponse<List<WarehouseResponse>> getWarehouses() {
        return ApiResponse.ok(warehouseService.getWarehouses());
    }

    @PostMapping
    public ApiResponse<WarehouseResponse> createWarehouse(@RequestBody WarehouseRequest request) {
        return ApiResponse.ok(warehouseService.createWarehouse(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<WarehouseResponse> updateWarehouse(
            @PathVariable Integer id,
            @RequestBody WarehouseRequest request
    ) {
        return ApiResponse.ok(warehouseService.updateWarehouse(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteWarehouse(@PathVariable Integer id) {
        return ApiResponse.ok(warehouseService.deleteWarehouse(id));
    }
}
