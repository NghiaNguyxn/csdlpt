package com.example.csdlpt.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LocalOrderItemRequest {
    Integer productId;
    Integer warehouseId;
    Integer quantity;
}
