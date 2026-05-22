package com.example.csdlpt.repository;

import java.math.BigDecimal;

public interface MultiWarehouseOrderLineProjection {

    Long getOrderId();

    Integer getProductId();

    String getProductName();

    Integer getWarehouseId();

    String getWarehouseCode();

    String getWarehouseName();

    String getSiteCode();

    Integer getQuantity();

    BigDecimal getPrice();

}
