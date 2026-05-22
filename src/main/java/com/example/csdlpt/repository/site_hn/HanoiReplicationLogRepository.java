package com.example.csdlpt.repository.site_hn;

import com.example.csdlpt.entity.ReplicationLog;
import com.example.csdlpt.enums.ReplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HanoiReplicationLogRepository extends JpaRepository<ReplicationLog, Integer> {
    List<ReplicationLog> findByStatusAndTargetSiteOrderByIdAsc(ReplicationStatus status, String targetSite);
}
