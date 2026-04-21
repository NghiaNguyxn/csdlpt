package com.example.csdlpt.repository.site_dn;

import com.example.csdlpt.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DanangCategoryRepository extends JpaRepository<Category, Integer> {
}
