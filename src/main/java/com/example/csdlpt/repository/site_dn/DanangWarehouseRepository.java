package com.example.csdlpt.repository.site_dn;

import com.example.csdlpt.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DanangWarehouseRepository extends JpaRepository<Warehouse, Integer> {
}
