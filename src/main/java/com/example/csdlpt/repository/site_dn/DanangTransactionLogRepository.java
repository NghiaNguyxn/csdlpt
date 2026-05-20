package com.example.csdlpt.repository.site_dn;

import com.example.csdlpt.repository.common.DistributedTransactionLogRepository;
import com.example.csdlpt.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DanangTransactionLogRepository extends DistributedTransactionLogRepository {
}
