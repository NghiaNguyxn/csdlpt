package com.example.csdlpt.repository.site_hcm;

import com.example.csdlpt.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HcmTransactionLogRepository extends JpaRepository<TransactionLog, String> {
}
