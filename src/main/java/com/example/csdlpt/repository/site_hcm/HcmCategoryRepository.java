package com.example.csdlpt.repository.site_hcm;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.csdlpt.entity.Category;

@Repository
public interface HcmCategoryRepository extends JpaRepository<Category, Integer> {
    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    @Modifying
    @Query(value = "INSERT INTO category (id, name) "
            + "VALUES (:id, :name) "
            + "ON CONFLICT (id) DO UPDATE SET "
            + "name = EXCLUDED.name", nativeQuery = true)
    void upsertCategory(@Param("id") Integer id, @Param("name") String name);
}
