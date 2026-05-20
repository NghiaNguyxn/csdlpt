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
public class QueryPerformanceResponse {

    Integer productId;
    Integer centralizedQuantity;
    Integer distributedQuantity;
    Long centralizedElapsedNanos;
    Long distributedElapsedNanos;
    Double centralizedElapsedMillis;
    Double distributedElapsedMillis;
    Integer centralizedRowsTransferred;
    Integer distributedRowsTransferred;
    Integer iterations;
    String centralizedStrategy;
    String distributedStrategy;

}
