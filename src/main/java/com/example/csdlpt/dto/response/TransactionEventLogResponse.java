package com.example.csdlpt.dto.response;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionEventLogResponse {

    Long id;
    String transactionId;
    String eventType;
    String actorRole;
    String siteCode;
    String resourceKey;
    String lockMode;
    String status;
    Long waitMillis;
    String message;
    LocalDateTime createdAt;
}
