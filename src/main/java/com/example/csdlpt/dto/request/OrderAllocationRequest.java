package com.example.csdlpt.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderAllocationRequest {
    String siteCode;
    Integer warehouseId;
    Integer quantity;
}
