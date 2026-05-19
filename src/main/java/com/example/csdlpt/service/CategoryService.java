package com.example.csdlpt.service;

import com.example.csdlpt.dto.request.CategoryRequest;
import com.example.csdlpt.dto.response.CategoryResponse;
import com.example.csdlpt.entity.Category;
import com.example.csdlpt.enums.ReplicationAction;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.site_dn.DanangCategoryRepository;
import com.example.csdlpt.repository.site_hcm.HcmCategoryRepository;
import com.example.csdlpt.repository.site_hn.HanoiCategoryRepository;
import com.example.csdlpt.repository.site_hn.HanoiProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final HanoiCategoryRepository hanoiCategoryRepository;
    private final DanangCategoryRepository danangCategoryRepository;
    private final HcmCategoryRepository hcmCategoryRepository;
    private final HanoiProductRepository hanoiProductRepository;
    private final ReplicationService replicationService;

    public List<CategoryResponse> getAllCategories() {
        List<CategoryResponse> responses = new ArrayList<>();
        hanoiCategoryRepository.findAll().forEach(category -> responses.add(toResponse(category, "HN")));
        return responses;
    }

    @Transactional(value = "hanoiTransactionManager")
    public CategoryResponse createCategory(CategoryRequest request) {
        validateName(request);
        if (hanoiCategoryRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new AppException(ErrorCode.CATEGORY_EXISTED);
        }

        Category saved = hanoiCategoryRepository.save(Category.builder()
                .name(request.getName().trim())
                .build());
        replicationService.logChange(saved.getId().longValue(), "CATEGORY", ReplicationAction.INSERT);
        return toResponse(saved, "HN_MASTER");
    }

    @Transactional(value = "hanoiTransactionManager")
    public CategoryResponse updateCategory(Integer id, CategoryRequest request) {
        validateName(request);
        Category category = hanoiCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        category.setName(request.getName().trim());
        Category saved = hanoiCategoryRepository.save(category);
        replicationService.logChange(saved.getId().longValue(), "CATEGORY", ReplicationAction.UPDATE);
        return toResponse(saved, "HN_MASTER");
    }

    @Transactional(value = "hanoiTransactionManager")
    public String deleteCategory(Integer id) {
        Category category = hanoiCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        if (hanoiProductRepository.existsByCategory_Id(id)) {
            throw new AppException(ErrorCode.DELETE_FAILED,
                    "Không thể xóa category vì vẫn còn product_basic tham chiếu category này");
        }
        hanoiCategoryRepository.delete(category);
        replicationService.logChange(id.longValue(), "CATEGORY", ReplicationAction.DELETE);
        return "Đã xóa category ở HN và ghi log đồng bộ sang DN, HCM";
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
