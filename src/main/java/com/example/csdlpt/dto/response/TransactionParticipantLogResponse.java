package com.example.csdlpt.dto.response;

import com.example.csdlpt.enums.TransactionStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionParticipantLogResponse {
    Integer id;
    String transactionId;
    String siteCode;
    Integer warehouseId;
    Integer productId;
    Integer quantity;
    TransactionStatus status;
    String message;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String sourceSite;
}
