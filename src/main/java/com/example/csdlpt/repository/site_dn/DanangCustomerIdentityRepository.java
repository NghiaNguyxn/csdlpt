package com.example.csdlpt.repository.site_dn;

import com.example.csdlpt.entity.CustomerIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DanangCustomerIdentityRepository extends JpaRepository<CustomerIdentity, Long> {
    Optional<CustomerIdentity> findByEmail(String email);

    @Modifying
    @Query(value = "INSERT INTO customer_identity (id, email, password, main_site_id) " +
            "VALUES (:id, :email, :password, :mainSiteId) " +
            "ON CONFLICT (id) DO UPDATE SET " +
            "email = EXCLUDED.email, password = EXCLUDED.password, main_site_id = EXCLUDED.main_site_id",
            nativeQuery = true)
    void replicateCustomerIdentity(@Param("id") Long id,
            @Param("email") String email,
            @Param("password") String password,
            @Param("mainSiteId") Integer mainSiteId);
}
