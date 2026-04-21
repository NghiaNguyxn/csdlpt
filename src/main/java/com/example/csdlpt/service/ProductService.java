package com.example.csdlpt.service;

import lombok.AccessLevel;

import com.example.csdlpt.entity.Category;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.csdlpt.context.SiteContextHolder;
import com.example.csdlpt.dto.request.ProductCreateRequest;
import com.example.csdlpt.dto.response.ProductBasicResponse;
import com.example.csdlpt.dto.response.ProductResponse;
import com.example.csdlpt.entity.ProductBasic;
import com.example.csdlpt.entity.ProductDetail;
import com.example.csdlpt.entity.ReplicationLog;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.mapper.ProductMapper;
import com.example.csdlpt.repository.site_dn.DanangProductRepository;
import com.example.csdlpt.repository.site_hcm.HcmProductRepository;
import com.example.csdlpt.repository.site_hn.HanoiCategoryRepository;
import com.example.csdlpt.repository.site_hn.HanoiProductDetailRepository;
import com.example.csdlpt.repository.site_hn.HanoiProductRepository;
import com.example.csdlpt.repository.site_hn.HanoiReplicationLogRepository;
import com.example.csdlpt.enums.ReplicationStatus;

import jakarta.transaction.Transactional;
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

    HanoiReplicationLogRepository hanoiReplicationLogRepository;

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {

        ProductBasic basic = productMapper.toProductBasic(request);
        ProductDetail detail = productMapper.toProductDetail(request);

        Category category = hanoiCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        basic.setCategory(category);
        detail.setProduct(basic);

        // 1. CHỈ LƯU VÀO MASTER (HÀ NỘI)
        ProductBasic savedBasic = hanoiProductRepository.save(basic);
        ProductDetail savedDetail = hanoiDetailRepository.save(detail);

        // 2. GHI LOG CHO CƠ CHẾ LAZY REPLICATION (DEFERRED UPDATE)
        ReplicationLog logDN = ReplicationLog.builder()
                .entityId(savedBasic.getId())
                .entityType("PRODUCT")
                .action("INSERT")
                .targetSite("DN")
                .status(ReplicationStatus.PENDING)
                .build();

        ReplicationLog logHCM = ReplicationLog.builder()
                .entityId(savedBasic.getId())
                .entityType("PRODUCT")
                .action("INSERT")
                .targetSite("HCM")
                .status(ReplicationStatus.PENDING)
                .build();

        hanoiReplicationLogRepository.saveAll(List.of(logDN, logHCM));

        return productMapper.toResponse(savedBasic, savedDetail);

    }

    public List<ProductBasicResponse> getAllProducts() {

        String localSiteCode = SiteContextHolder.getCurrentSite();
        log.info("Thực thi getProducts tại Local Site: {}", localSiteCode);

        List<ProductBasic> basics;

        if ("DN".equals(localSiteCode)) {
            basics = danangProductRepository.findAll();
        } else if ("HCM".equals(localSiteCode)) {
            basics = hcmProductRepository.findAll();
        } else {
            // Mặc định hoặc HN
            basics = hanoiProductRepository.findAll();
        }

        return productMapper.toBasicResponses(basics);

    }

    public ProductResponse getProductById(Integer id) {

        String localSiteCode = SiteContextHolder.getCurrentSite();
        log.info("Thực thi getProduct tại Local Site: {}", localSiteCode);

        ProductBasic basic;
        ProductDetail detail;

        if ("DN".equals(localSiteCode)) {
            basic = danangProductRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        } else if ("HCM".equals(localSiteCode)) {
            basic = hcmProductRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        } else {
            basic = hanoiProductRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        }

        detail = hanoiDetailRepository.findByProductId(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        return productMapper.toResponse(basic, detail);

    }

}
