package com.example.csdlpt.repository.site_hcm;

import com.example.csdlpt.entity.ProductBasic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HcmProductRepository extends JpaRepository<ProductBasic, Integer> {
}
