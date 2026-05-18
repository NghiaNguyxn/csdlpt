package com.example.csdlpt.dto.request;

import java.math.BigDecimal;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FallbackOrderRequest {

    Long orderId;
    Long customerId;
    Integer productId;
    Integer primaryWarehouseId;
    Integer quantity;
    BigDecimal price;

}
