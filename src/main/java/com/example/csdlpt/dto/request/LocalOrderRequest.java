package com.example.csdlpt.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LocalOrderRequest {
    Long orderId;
    Long customerId;
    Integer warehouseId;
    String siteCode;
    List<LocalOrderItemRequest> items;
}
