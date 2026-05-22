package com.example.csdlpt.repository.site_hn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.csdlpt.entity.TransactionParticipantLog;

@Repository
public interface HanoiTransactionParticipantLogRepository extends JpaRepository<TransactionParticipantLog, Integer> {
}
