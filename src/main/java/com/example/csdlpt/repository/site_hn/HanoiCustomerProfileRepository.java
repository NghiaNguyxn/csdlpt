package com.example.csdlpt.repository.site_hn;

import com.example.csdlpt.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HanoiCustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
}
