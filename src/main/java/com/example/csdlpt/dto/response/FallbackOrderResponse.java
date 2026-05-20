package com.example.csdlpt.dto.response;

import java.util.List;

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
public class FallbackOrderResponse {

    Long orderId;
    String transactionId;
    Long customerId;
    Integer productId;
    Integer requestedQuantity;
    Integer fulfilledQuantity;
    String primarySiteCode;
    Integer primaryWarehouseId;
    Boolean fallbackUsed;
    String q2ReuseNote;
    List<FallbackOrderAllocationResponse> allocations;

}
