package com.example.csdlpt.repository.site_dn;

import com.example.csdlpt.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DanangCustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
}
