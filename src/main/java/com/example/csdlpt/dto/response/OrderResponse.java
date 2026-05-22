package com.example.csdlpt.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.example.csdlpt.enums.OrderStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponse {
    Long id;
    Long customerId;
    LocalDateTime orderDate;
    OrderStatus status;
    String siteCode;
    String sourceSite;
    BigDecimal totalAmount;
    List<OrderDetailResponse> details;
}
