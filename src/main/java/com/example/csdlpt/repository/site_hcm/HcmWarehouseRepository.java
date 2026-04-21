package com.example.csdlpt.repository.site_hcm;

import com.example.csdlpt.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HcmWarehouseRepository extends JpaRepository<Warehouse, Integer> {
}
