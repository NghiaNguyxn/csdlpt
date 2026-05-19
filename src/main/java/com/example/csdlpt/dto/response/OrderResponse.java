package com.example.csdlpt.dto.response;

import com.example.csdlpt.enums.OrderStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    Integer warehouseId;
    Integer siteId;
    String sourceSite;
    BigDecimal totalAmount;
    List<OrderDetailResponse> details;
    String transactionId;
}
