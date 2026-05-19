package com.example.csdlpt.dto.response;

import com.example.csdlpt.enums.TransactionStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionLogResponse {
    String transactionId;
    TransactionStatus status;
    String participants;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String sourceSite;
}
