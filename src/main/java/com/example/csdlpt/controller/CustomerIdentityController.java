package com.example.csdlpt.controller;

import com.example.csdlpt.dto.request.CustomerIdentityRequest;
import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.CustomerIdentityResponse;
import com.example.csdlpt.service.CustomerIdentityService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer-identities")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerIdentityController {

    CustomerIdentityService customerIdentityService;

    @PostMapping
    public ApiResponse<CustomerIdentityResponse> createCustomerIdentity(
            @RequestBody CustomerIdentityRequest request) {
        return ApiResponse.ok(customerIdentityService.createCustomerIdentity(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerIdentityResponse> updateCustomerIdentity(
            @PathVariable Long id,
            @RequestBody CustomerIdentityRequest request) {
        return ApiResponse.ok(customerIdentityService.updateCustomerIdentity(id, request));
    }

}
