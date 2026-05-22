package com.example.csdlpt.repository.site_hcm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.csdlpt.entity.TransactionParticipantLog;

@Repository
public interface HcmTransactionParticipantLogRepository extends JpaRepository<TransactionParticipantLog, Integer> {
}
