package com.example.csdlpt.controller;

import com.example.csdlpt.dto.request.RaceConditionRequest;
import com.example.csdlpt.dto.response.ApiResponse;
import com.example.csdlpt.dto.response.RaceConditionResponse;
import com.example.csdlpt.service.RaceConditionTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    private final RaceConditionTestService raceConditionTestService;

    @PostMapping("/race-condition")
    public ApiResponse<RaceConditionResponse> runRaceConditionTest(@RequestBody RaceConditionRequest request) {
        return ApiResponse.ok(raceConditionTestService.runRaceConditionTest(request));
    }
}
