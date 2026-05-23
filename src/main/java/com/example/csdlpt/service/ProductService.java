package com.example.csdlpt.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.csdlpt.context.SiteContextHolder;
import com.example.csdlpt.dto.request.ProductRequest;
import com.example.csdlpt.dto.response.ProductBasicResponse;
import com.example.csdlpt.dto.response.ProductResponse;
import com.example.csdlpt.entity.Category;
import com.example.csdlpt.entity.ProductBasic;
import com.example.csdlpt.entity.ProductDetail;
import com.example.csdlpt.enums.ReplicationAction;
import com.example.csdlpt.enums.SiteCode;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.mapper.ProductMapper;
import com.example.csdlpt.repository.site_dn.DanangProductRepository;
import com.example.csdlpt.repository.site_hcm.HcmProductRepository;
import com.example.csdlpt.repository.site_hn.HanoiCategoryRepository;
import com.example.csdlpt.repository.site_hn.HanoiProductDetailRepository;
import com.example.csdlpt.repository.site_hn.HanoiProductRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductService {

    HanoiProductRepository hanoiProductRepository;
    DanangProductRepository danangProductRepository;
    HcmProductRepository hcmProductRepository;
    HanoiProductDetailRepository hanoiDetailRepository;
    HanoiCategoryRepository hanoiCategoryRepository;
    ProductMapper productMapper;
    ReplicationService replicationService;

    @Transactional("hanoiTransactionManager")
    public ProductResponse createProduct(ProductRequest request) {
        // Product dùng phân mảnh dọc: basic ở các site, detail chỉ lưu tại master HN.
        ProductBasic basic = productMapper.toProductBasic(request);
        ProductDetail detail = productMapper.toProductDetail(request);

        Category category = hanoiCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        basic.setCategory(category);
        detail.setProduct(basic);

        ProductBasic savedBasic = hanoiProductRepository.save(basic);
        hanoiDetailRepository.save(detail);

        // Lazy replication: chỉ ghi log tại HN, ReplicationJob sẽ đẩy product_basic sang DN/HCM sau.
        replicationService.logChange(savedBasic.getId().longValue(), "PRODUCT", ReplicationAction.INSERT);

        return productMapper.toResponse(savedBasic, detail);
    }

    public List<ProductBasicResponse> getAllProducts() {
        SiteCode localSiteCode = SiteContextHolder.getCurrentSite();
        log.info("Thực thi getProducts tại Local Site: {}", localSiteCode);

        // Đọc product_basic từ site hiện tại để thể hiện dữ liệu replicated tại từng site.
        List<ProductBasic> basics;
        if (SiteCode.DN == localSiteCode) {
            basics = danangProductRepository.findByIsActiveTrue();
        } else if (SiteCode.HCM == localSiteCode) {
            basics = hcmProductRepository.findByIsActiveTrue();
        } else {
            basics = hanoiProductRepository.findByIsActiveTrue();
        }

        return productMapper.toBasicResponses(basics);
    }

    public ProductResponse getProductById(Integer id) {
        SiteCode localSiteCode = SiteContextHolder.getCurrentSite();
        log.info("Thực thi getProduct tại Local Site: {}", localSiteCode);

        // Basic lấy theo site hiện tại, còn detail lấy từ HN vì đây là mảnh dọc đặt tại master.
        ProductBasic basic;
        if (SiteCode.DN == localSiteCode) {
            basic = danangProductRepository.findByIdAndIsActiveTrue(id)
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        } else if (SiteCode.HCM == localSiteCode) {
            basic = hcmProductRepository.findByIdAndIsActiveTrue(id)
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        } else {
            basic = hanoiProductRepository.findByIdAndIsActiveTrue(id)
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        }

        ProductDetail detail = hanoiDetailRepository.findByProductId(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        return productMapper.toResponse(basic, detail);
    }

    @Transactional("hanoiTransactionManager")
    public ProductResponse updateProduct(Integer id, ProductRequest request) {
        ProductBasic basic = hanoiProductRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductDetail detail = hanoiDetailRepository.findByProductId(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Category category = hanoiCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        basic.setName(request.getName());
        basic.setPrice(request.getPrice());
        basic.setCategory(category);
        detail.setDescription(request.getDescription());

        hanoiProductRepository.save(basic);
        hanoiDetailRepository.save(detail);

        // Update vẫn đi qua master HN, sau đó phát tán bằng replication log.
        replicationService.logChange(basic.getId().longValue(), "PRODUCT", ReplicationAction.UPDATE);

        return productMapper.toResponse(basic, detail);
    }

    @Transactional("hanoiTransactionManager")
    public String deleteProduct(Integer id) {
        ProductBasic basic = hanoiProductRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        basic.setIsActive(false);
        hanoiProductRepository.save(basic);

        // Soft delete cũng cần replicate is_active=false sang các bản sao.
        replicationService.logChange(basic.getId().longValue(), "PRODUCT", ReplicationAction.DELETE);

        return "Xóa sản phẩm thành công (Soft Delete)!";
    }
}
