package com.example.csdlpt.repository.site_hn;

import com.example.csdlpt.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HanoiTransactionLogRepository extends JpaRepository<TransactionLog, String> {
}
