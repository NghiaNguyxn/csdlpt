package com.example.csdlpt.dto.response;

import java.util.List;

import com.example.csdlpt.enums.OrderStatus;
import com.example.csdlpt.enums.SiteCode;
import com.example.csdlpt.enums.TransactionStatus;

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
public class DistributedOrderResponse {

    Long orderId;
    String transactionId;
    SiteCode localSiteCode;
    OrderStatus orderStatus;
    TransactionStatus transactionStatus;
    List<OrderAllocationResponse> allocations;
}
