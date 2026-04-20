package com.example.csdlpt.repository.site_dn;

import com.example.csdlpt.entity.ProductBasic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DanangProductRepository extends JpaRepository<ProductBasic, Integer> {
}
