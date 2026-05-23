package com.example.csdlpt.controller;

import com.example.csdlpt.dto.request.CustomerRequest;
import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.CustomerResponse;
import com.example.csdlpt.service.Customer.CustomerService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerController {

    CustomerService customerService;

    @GetMapping
    public ApiResponse<List<CustomerResponse>> findAllCustomers() {
        return ApiResponse.ok(customerService.findAllCustomers());
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerResponse> findCustomerById(@PathVariable Long id) {
        return ApiResponse.ok(customerService.findCustomerById(id));
    }

    @PostMapping
    public ApiResponse<CustomerResponse> createCustomer(@RequestBody CustomerRequest request) {
        return ApiResponse.ok(customerService.createCustomer(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @RequestBody CustomerRequest request) {
        return ApiResponse.ok(customerService.updateCustomer(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ApiResponse.ok(null, "Deleted");
    }
}
