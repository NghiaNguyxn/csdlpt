package com.example.csdlpt.repository.site_dn;

import com.example.csdlpt.entity.CustomerProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DanangCustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    @Override
    @EntityGraph(attributePaths = {"identity", "mainSite"})
    List<CustomerProfile> findAll();

    @Override
    @EntityGraph(attributePaths = {"identity", "mainSite"})
    Optional<CustomerProfile> findById(Long id);
}
