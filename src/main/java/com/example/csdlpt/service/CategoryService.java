package com.example.csdlpt.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.csdlpt.dto.request.CategoryRequest;
import com.example.csdlpt.dto.response.CategoryResponse;
import com.example.csdlpt.entity.Category;
import com.example.csdlpt.enums.ReplicationAction;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.site_dn.DanangCategoryRepository;
import com.example.csdlpt.repository.site_dn.DanangProductRepository;
import com.example.csdlpt.repository.site_hcm.HcmCategoryRepository;
import com.example.csdlpt.repository.site_hcm.HcmProductRepository;
import com.example.csdlpt.repository.site_hn.HanoiCategoryRepository;
import com.example.csdlpt.repository.site_hn.HanoiProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final HanoiCategoryRepository hanoiCategoryRepository;
    private final DanangCategoryRepository danangCategoryRepository;
    private final HcmCategoryRepository hcmCategoryRepository;

    private final HanoiProductRepository hanoiProductRepository;
    private final DanangProductRepository danangProductRepository;
    private final HcmProductRepository hcmProductRepository;

    private final ReplicationService replicationService;

    public List<CategoryResponse> getAllCategories() {
        Map<Integer, CategoryResponse> unique = new LinkedHashMap<>();
        hanoiCategoryRepository.findAll().forEach(category -> unique.putIfAbsent(category.getId(), toResponse(category, "HN")));
        danangCategoryRepository.findAll().forEach(category -> unique.putIfAbsent(category.getId(), toResponse(category, "DN")));
        hcmCategoryRepository.findAll().forEach(category -> unique.putIfAbsent(category.getId(), toResponse(category, "HCM")));
        return new ArrayList<>(unique.values()).stream()
                .sorted(Comparator.comparing(CategoryResponse::getId))
                .toList();
    }

    @Transactional("hanoiTransactionManager")
    public CategoryResponse createCategory(CategoryRequest request) {
        validateName(request);
        String name = request.getName().trim();
        if (hanoiCategoryRepository.existsByNameIgnoreCase(name)) {
            throw new AppException(ErrorCode.INVALID_KEY, "Tên category đã tồn tại ở master HN");
        }
        Category saved = hanoiCategoryRepository.save(Category.builder().name(name).build());
        replicationService.logChange(saved.getId().longValue(), "CATEGORY", ReplicationAction.INSERT);
        return toResponse(saved, "HN_MASTER_PENDING_REPLICATION");
    }

    @Transactional("hanoiTransactionManager")
    public CategoryResponse updateCategory(Integer id, CategoryRequest request) {
        validateName(request);
        Category category = hanoiCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        String name = request.getName().trim();
        hanoiCategoryRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.INVALID_KEY, "Tên category đã tồn tại ở category khác tại master HN");
                });
        category.setName(name);
        Category saved = hanoiCategoryRepository.save(category);
        replicationService.logChange(saved.getId().longValue(), "CATEGORY", ReplicationAction.UPDATE);
        return toResponse(saved, "HN_MASTER_PENDING_REPLICATION");
    }

    @Transactional("hanoiTransactionManager")
    public String deleteCategory(Integer id) {
        Category category = hanoiCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        boolean usedByProduct = hanoiProductRepository.existsByCategory_Id(id)
                || danangProductRepository.existsByCategory_Id(id)
                || hcmProductRepository.existsByCategory_Id(id);
        if (usedByProduct) {
            throw new AppException(ErrorCode.DELETE_FAILED,
                    "Không thể xóa category vì vẫn còn product_basic tham chiếu category này");
        }
        hanoiCategoryRepository.delete(category);
        replicationService.logChange(id.longValue(), "CATEGORY", ReplicationAction.DELETE);
        return "Đã xóa category ở master HN và ghi log lazy replication sang DN/HCM";
    }

    private void validateName(CategoryRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY, "Tên category không được để trống");
        }
    }

    private CategoryResponse toResponse(Category category, String sourceSite) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .sourceSite(sourceSite)
                .build();
    }
}
