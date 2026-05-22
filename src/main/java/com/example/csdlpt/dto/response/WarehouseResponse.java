package com.example.csdlpt.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WarehouseResponse {
    Integer id;
    String code;
    String name;
    String location;
    String region;
    Integer siteId;
    String siteCode;
    String sourceSite;
}
