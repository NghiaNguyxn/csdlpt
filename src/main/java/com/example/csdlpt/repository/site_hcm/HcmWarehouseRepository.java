package com.example.csdlpt.repository.site_hcm;

import com.example.csdlpt.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import java.util.Optional;

@Repository
public interface HcmWarehouseRepository extends JpaRepository<Warehouse, Integer> {
    Optional<Warehouse> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Transactional("hcmTransactionManager")
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO warehouse (id, code, name, location, region, site_id)
        VALUES (:id, :code, :name, :location, :region, :siteId)
        ON CONFLICT (id) DO UPDATE SET
            code = EXCLUDED.code,
            name = EXCLUDED.name,
            location = EXCLUDED.location,
            region = EXCLUDED.region,
            site_id = EXCLUDED.site_id
        """, nativeQuery = true)
    void upsertWarehouse(@Param("id") Integer id,
                         @Param("code") String code,
                         @Param("name") String name,
                         @Param("location") String location,
                         @Param("region") String region,
                         @Param("siteId") Integer siteId);

}
