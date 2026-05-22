package com.example.csdlpt.dto.request;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LocalOrderRequest {
    Long customerId;
    List<LocalOrderItemRequest> items;
}
