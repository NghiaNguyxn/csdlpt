package com.example.csdlpt.repository.site_hn;

import com.example.csdlpt.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HanoiWarehouseRepository extends JpaRepository<Warehouse, Integer> {
    Optional<Warehouse> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
