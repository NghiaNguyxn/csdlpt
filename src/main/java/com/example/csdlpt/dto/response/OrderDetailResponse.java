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
public class OrderDetailResponse {
    Long orderId;
    Integer productId;
    String productName;
    Integer warehouseId;
    String warehouseCode;
    Integer quantity;
    BigDecimal price;
    BigDecimal lineTotal;
}
