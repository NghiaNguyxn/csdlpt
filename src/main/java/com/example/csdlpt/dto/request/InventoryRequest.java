package com.example.csdlpt.dto.request;

import com.example.csdlpt.enums.SiteCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryRequest {

    Integer productId;
    Integer warehouseId;
    Integer quantity;
    SiteCode targetSite;

}
