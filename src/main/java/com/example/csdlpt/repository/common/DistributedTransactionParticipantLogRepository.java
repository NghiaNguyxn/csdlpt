package com.example.csdlpt.repository.common;

import com.example.csdlpt.entity.TransactionParticipantLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface DistributedTransactionParticipantLogRepository extends JpaRepository<TransactionParticipantLog, Integer> {
    List<TransactionParticipantLog> findByTransactionId(String transactionId);
}
