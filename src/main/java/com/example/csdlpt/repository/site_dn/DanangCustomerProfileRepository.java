package com.example.csdlpt.repository.site_dn;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO customer_profile (id, name, phone, address)
            VALUES (:id, :name, :phone, :address)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                phone = EXCLUDED.phone,
                address = EXCLUDED.address
            """, nativeQuery = true)
    void upsertProfile(@Param("id") Long id,
                       @Param("name") String name,
                       @Param("phone") String phone,
                       @Param("address") String address);
}
