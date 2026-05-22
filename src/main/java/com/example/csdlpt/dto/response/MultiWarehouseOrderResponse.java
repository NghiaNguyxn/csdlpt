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
public class MultiWarehouseOrderResponse {

    Long orderId;
    /** Số kho khác nhau phục vụ đơn hàng (COUNT DISTINCT warehouse_id). */
    Integer warehouseCount;
    /** Số site khác nhau phục vụ đơn hàng (COUNT DISTINCT site_id). */
    Integer siteCount;
    List<String> siteCodes;
    List<String> warehouseCodes;
    List<DistributedOrderLineResponse> lines;

}
