package com.example.csdlpt.controller;

import com.example.csdlpt.dto.request.ProductRequest;
import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.ProductBasicResponse;
import com.example.csdlpt.dto.response.ProductResponse;
import com.example.csdlpt.service.ProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductController {

    ProductService productService;

    @PostMapping()
    public ApiResponse<ProductResponse> createProduct(@RequestBody ProductRequest request) {

        return ApiResponse.ok(productService.createProduct(request));

    }

    @GetMapping()
    public ApiResponse<List<ProductBasicResponse>> getProducts() {

        return ApiResponse.ok(productService.getAllProducts());

    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Integer id) {

        return ApiResponse.ok(productService.getProductById(id));

    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable Integer id, @RequestBody ProductRequest request) {

        return ApiResponse.ok(productService.updateProduct(id, request));

    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteProduct(@PathVariable Integer id) {

        return ApiResponse.ok(productService.deleteProduct(id));

    }

}
