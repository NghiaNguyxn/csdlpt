package com.example.csdlpt.repository.projection;

import java.math.BigDecimal;

public interface MonthlyRevenueProjection {
    Integer getMonth();
    Integer getWarehouseId();
    BigDecimal getRevenue();
}
