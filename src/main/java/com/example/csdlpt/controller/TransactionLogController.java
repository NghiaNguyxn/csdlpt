package com.example.csdlpt.controller;

import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.TransactionLogResponse;
import com.example.csdlpt.dto.response.TransactionParticipantLogResponse;
import com.example.csdlpt.service.TransactionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionLogController {
    private final TransactionLogService transactionLogService;

    @GetMapping
    public ApiResponse<List<TransactionLogResponse>> getTransactions() {
        return ApiResponse.ok(transactionLogService.getTransactions());
    }

    @GetMapping("/{id}")
    public ApiResponse<TransactionLogResponse> getTransaction(@PathVariable String id) {
        return ApiResponse.ok(transactionLogService.getTransaction(id));
    }

    @GetMapping("/{id}/participants")
    public ApiResponse<List<TransactionParticipantLogResponse>> getParticipantLogs(@PathVariable String id) {
        return ApiResponse.ok(transactionLogService.getParticipantLogs(id));
    }
}
