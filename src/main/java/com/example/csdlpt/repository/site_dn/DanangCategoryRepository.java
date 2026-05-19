package com.example.csdlpt.repository.site_dn;

import com.example.csdlpt.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DanangCategoryRepository extends JpaRepository<Category, Integer> {
    @Modifying
    @Query(value = "INSERT INTO category (id, name) VALUES (:id, :name) " +
            "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name", nativeQuery = true)
    void replicateCategory(@Param("id") Integer id, @Param("name") String name);

    @Modifying
    @Query(value = "DELETE FROM category WHERE id = :id", nativeQuery = true)
    void deleteCategoryReplica(@Param("id") Integer id);
}
