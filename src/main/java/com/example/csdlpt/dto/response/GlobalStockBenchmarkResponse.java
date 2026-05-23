package com.example.csdlpt.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GlobalStockBenchmarkResponse {

    Integer productId;
    Integer iterations;
    Integer warmupIterations;
    Integer centralizedQuantity;
    Integer distributedQuantity;
    BenchmarkMetricResponse centralized;
    BenchmarkMetricResponse distributed;
}
