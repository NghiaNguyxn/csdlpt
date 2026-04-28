package com.example.csdlpt.repository.site_hcm;

import com.example.csdlpt.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HcmCustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
}
