package com.example.csdlpt.repository.site_hcm;

import com.example.csdlpt.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HcmCategoryRepository extends JpaRepository<Category, Integer> {
}
