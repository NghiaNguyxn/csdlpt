package com.example.csdlpt.repository.site_dn;

import com.example.csdlpt.entity.Warehouse;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DanangWarehouseRepository extends JpaRepository<Warehouse, Integer> {
    @Override
    @EntityGraph(attributePaths = "site")
    List<Warehouse> findAll();

    Optional<Warehouse> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Transactional("danangTransactionManager")
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

    @Query(value = """
        SELECT setval(
            'warehouse_id_seq',
            GREATEST(
                (SELECT COALESCE(MAX(id), 1) FROM warehouse),
                (SELECT last_value FROM warehouse_id_seq)
            ),
            true
        )
        """, nativeQuery = true)
    Long syncWarehouseSequence();

}
