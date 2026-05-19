package com.example.csdlpt.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WarehouseRequest {
    String code;
    String name;
    String location;
    String region;
    String siteCode;
    Integer siteId;
}
