package com.example.csdlpt.repository.site_hcm;

import com.example.csdlpt.entity.ProductBasic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import java.util.List;
import java.util.Optional;

@Repository
public interface HcmProductRepository extends JpaRepository<ProductBasic, Integer> {
    List<ProductBasic> findByIsActiveTrue();
    Optional<ProductBasic> findByIdAndIsActiveTrue(Integer id);
    @Modifying
    @Query(value = "INSERT INTO product_basic (id, name, price, category_id, is_active) " +
                   "VALUES (:id, :name, :price, :categoryId, :isActive) " +
                   "ON CONFLICT (id) DO UPDATE SET " +
                   "name = EXCLUDED.name, price = EXCLUDED.price, category_id = EXCLUDED.category_id, is_active = EXCLUDED.is_active",
           nativeQuery = true)
    void replicateProduct(@Param("id") Integer id, @Param("name") String name,
                          @Param("price") BigDecimal price, @Param("categoryId") Integer categoryId, @Param("isActive") Boolean isActive);
}
