package com.example.csdlpt.repository.site_hn;

import com.example.csdlpt.entity.TransactionEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HanoiTransactionEventLogRepository extends JpaRepository<TransactionEventLog, Long> {

    List<TransactionEventLog> findTop100ByOrderByCreatedAtDesc();

    List<TransactionEventLog> findByTransactionIdOrderByCreatedAtAsc(String transactionId);
}
