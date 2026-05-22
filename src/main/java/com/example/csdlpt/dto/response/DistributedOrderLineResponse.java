package com.example.csdlpt.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DistributedOrderLineResponse {

    Long orderId;
    String siteCode;
    Integer warehouseId;
    String warehouseCode;
    String warehouseName;
    Integer productId;
    String productName;
    Integer quantity;
    BigDecimal price;

}
