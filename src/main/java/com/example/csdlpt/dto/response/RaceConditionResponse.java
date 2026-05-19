package com.example.csdlpt.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RaceConditionResponse {
    String siteCode;
    Integer productId;
    Integer initialQuantity;
    Integer finalQuantity;
    Integer successCount;
    Integer failedCount;
    List<String> logs;
}
