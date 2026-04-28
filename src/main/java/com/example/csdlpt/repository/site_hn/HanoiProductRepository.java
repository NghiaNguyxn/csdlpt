package com.example.csdlpt.repository.site_hn;

import com.example.csdlpt.entity.ProductBasic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HanoiProductRepository extends JpaRepository<ProductBasic, Integer> {
    List<ProductBasic> findByIsActiveTrue();
    Optional<ProductBasic> findByIdAndIsActiveTrue(Integer id);
}
