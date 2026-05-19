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
public class MultiOrderRequest {
    Long orderId;
    Long customerId;
    String mainSite;
    Integer mainWarehouseId;
    List<MultiOrderItemRequest> items;
}
