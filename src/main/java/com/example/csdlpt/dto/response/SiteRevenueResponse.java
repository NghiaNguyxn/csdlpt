package com.example.csdlpt.dto.response;

import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SiteRevenueResponse {
    Integer year;
    String siteCode;
    BigDecimal revenue;
}
