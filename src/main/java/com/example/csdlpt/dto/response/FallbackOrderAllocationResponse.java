package com.example.csdlpt.dto.response;

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
public class FallbackOrderAllocationResponse {

    String siteCode;
    Integer warehouseId;
    String warehouseCode;
    Integer quantity;
    Integer remainingStock;
    Boolean fallback;

}
