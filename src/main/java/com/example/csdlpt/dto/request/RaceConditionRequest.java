package com.example.csdlpt.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RaceConditionRequest {
    String siteCode;
    Integer warehouseId;
    Integer productId;
    Integer initialQuantity;
    Integer quantityPerOrder;
    Integer threadCount;
}
