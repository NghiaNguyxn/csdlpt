package com.example.csdlpt.repository.site_hcm;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.csdlpt.entity.ReplicationLog;
import com.example.csdlpt.enums.ReplicationStatus;

@Repository
public interface HcmReplicationLogRepository extends JpaRepository<ReplicationLog, Integer> {
    List<ReplicationLog> findByEntityTypeAndStatus(String entityType, ReplicationStatus status);
}
