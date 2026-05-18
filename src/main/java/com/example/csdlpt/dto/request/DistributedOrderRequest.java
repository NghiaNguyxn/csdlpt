package com.example.csdlpt.dto.request;

import java.util.List;

import com.example.csdlpt.enums.SiteCode;

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
public class DistributedOrderRequest {

    Long customerId;

    List<DistributedOrderItemRequest> items;

    // Dùng để demo participant chủ động Vote NO trong pha PREPARE của 2PC.
    SiteCode simulateVoteNoSite;
}
