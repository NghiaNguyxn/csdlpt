package com.example.csdlpt.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
    List<SiteRevenueResponse> siteBreakdown;
}
