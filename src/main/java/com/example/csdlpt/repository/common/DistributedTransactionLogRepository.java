package com.example.csdlpt.repository.common;

import com.example.csdlpt.entity.TransactionLog;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.jpa.repository.JpaRepository;

@NoRepositoryBean
public interface DistributedTransactionLogRepository extends JpaRepository<TransactionLog, String> {
}
