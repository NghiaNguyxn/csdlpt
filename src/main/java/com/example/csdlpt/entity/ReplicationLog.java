package com.example.csdlpt.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Builder.Default;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import com.example.csdlpt.enums.ReplicationStatus;

@Entity
@Table(name = "replication_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "target_site", nullable = false, length = 20)
    private String targetSite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Default
    private ReplicationStatus status = ReplicationStatus.PENDING;

    @Column(name = "retry_count")
    @Default
    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}
