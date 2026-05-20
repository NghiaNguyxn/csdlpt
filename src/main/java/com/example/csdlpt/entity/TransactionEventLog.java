package com.example.csdlpt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_event_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "actor_role", nullable = false, length = 30)
    private String actorRole;

    @Column(name = "site_code", length = 20)
    private String siteCode;

    @Column(name = "resource_key", length = 120)
    private String resourceKey;

    @Column(name = "lock_mode", length = 20)
    private String lockMode;

    @Column(length = 30)
    private String status;

    @Column(name = "wait_millis")
    private Long waitMillis;

    @Column(columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
