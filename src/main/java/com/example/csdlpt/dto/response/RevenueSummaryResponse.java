package com.example.csdlpt.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RevenueSummaryResponse {
    Integer year;
    BigDecimal totalRevenue;
    List<MonthlyRevenueResponse> details;
}
