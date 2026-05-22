package com.example.csdlpt.repository.site_dn;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.csdlpt.entity.CustomerProfile;

@Repository
public interface DanangCustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    @Override
    @EntityGraph(attributePaths = {"identity"})
    List<CustomerProfile> findAll();

    @Override
    @EntityGraph(attributePaths = {"identity"})
    Optional<CustomerProfile> findById(Long id);
}
