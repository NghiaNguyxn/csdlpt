package com.example.csdlpt.repository.site_dn;

import com.example.csdlpt.entity.ProductBasic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface DanangProductRepository extends JpaRepository<ProductBasic, Integer> {
    @Modifying
    @Query(value = "INSERT INTO product_basic (id, name, price, category_id) " +
            "VALUES (:id, :name, :price, :categoryId) " +
            "ON CONFLICT (id) DO UPDATE SET " +
            "name = EXCLUDED.name, price = EXCLUDED.price, category_id = EXCLUDED.category_id", nativeQuery = true)
    void replicateProduct(@Param("id") Integer id, @Param("name") String name,
            @Param("price") BigDecimal price, @Param("categoryId") Integer categoryId);
}
