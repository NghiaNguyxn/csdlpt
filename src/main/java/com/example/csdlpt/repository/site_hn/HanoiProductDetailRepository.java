package com.example.csdlpt.repository.site_hn;

import com.example.csdlpt.entity.ProductDetail;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HanoiProductDetailRepository extends JpaRepository<ProductDetail, Integer> {

    Optional<ProductDetail> findByProductId(Integer id);
}
