package com.example.csdlpt.repository.site_dn;

import com.example.csdlpt.repository.common.DistributedTransactionParticipantLogRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.csdlpt.entity.TransactionParticipantLog;

@Repository
public interface DanangTransactionParticipantLogRepository extends DistributedTransactionParticipantLogRepository {
}
