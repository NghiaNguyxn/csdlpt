package com.example.csdlpt.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MonthlyRevenueResponse {
    Integer year;
    Integer month;
    Integer warehouseId;
    String warehouseCode;
    String siteCode;
    BigDecimal revenue;
}
