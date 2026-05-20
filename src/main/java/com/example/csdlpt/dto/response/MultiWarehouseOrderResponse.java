package com.example.csdlpt.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MultiWarehouseOrderResponse {

    Long orderId;
    Integer warehouseCount;
    List<String> siteCodes;
    List<String> warehouseCodes;
    List<DistributedOrderLineResponse> lines;

}
