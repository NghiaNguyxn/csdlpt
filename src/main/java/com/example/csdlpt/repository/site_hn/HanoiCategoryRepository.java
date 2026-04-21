package com.example.csdlpt.repository.site_hn;

import com.example.csdlpt.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HanoiCategoryRepository extends JpaRepository<Category, Integer> {
}
