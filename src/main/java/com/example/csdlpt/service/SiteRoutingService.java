package com.example.csdlpt.service;

import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.entity.InventoryId;
import com.example.csdlpt.entity.ProductBasic;
import com.example.csdlpt.entity.Warehouse;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.site_dn.DanangInventoryRepository;
import com.example.csdlpt.repository.site_dn.DanangProductRepository;
import com.example.csdlpt.repository.site_dn.DanangWarehouseRepository;
import com.example.csdlpt.repository.site_hcm.HcmInventoryRepository;
import com.example.csdlpt.repository.site_hcm.HcmProductRepository;
import com.example.csdlpt.repository.site_hcm.HcmWarehouseRepository;
import com.example.csdlpt.repository.site_hn.HanoiInventoryRepository;
import com.example.csdlpt.repository.site_hn.HanoiProductRepository;
import com.example.csdlpt.repository.site_hn.HanoiWarehouseRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.stream.Stream;

import com.example.csdlpt.repository.common.DistributedInventoryRepository;
import org.springframework.stereotype.Service;

import com.example.csdlpt.enums.SiteCode;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SiteRoutingService {

    
    HanoiProductRepository hanoiProductRepository;
    HanoiWarehouseRepository hanoiWarehouseRepository;
    HanoiInventoryRepository hanoiInventoryRepository;

    
    DanangProductRepository danangProductRepository;
    DanangWarehouseRepository danangWarehouseRepository;
    DanangInventoryRepository danangInventoryRepository;

    
    HcmProductRepository hcmProductRepository;
    HcmWarehouseRepository hcmWarehouseRepository;
    HcmInventoryRepository hcmInventoryRepository;

    
    public ProductBasic findProductBySite(Integer productId, SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangProductRepository.findById(productId)
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
            case HCM -> hcmProductRepository.findById(productId)
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
            default -> hanoiProductRepository.findById(productId)
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        };
    }

    
    public Warehouse findWarehouseBySite(Integer warehouseId, SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangWarehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND, "Không tìm thấy kho tại DN"));
            case HCM -> hcmWarehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND, "Không tìm thấy kho tại HCM"));
            default -> hanoiWarehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND, "Không tìm thấy kho tại HN"));
        };
    }

    
    public List<Warehouse> findAllWareHouseBySite(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangWarehouseRepository.findAll();
            case HCM -> hcmWarehouseRepository.findAll();
            default -> hanoiWarehouseRepository.findAll();
        };
    }

    public List<Warehouse> findAllWareHouse() {
        return Stream.concat(danangWarehouseRepository.findAll().stream(),
                Stream.concat(hanoiWarehouseRepository.findAll().stream(),
                        hcmWarehouseRepository.findAll().stream()))
                .toList();
    }

    
    public DistributedInventoryRepository getInventoryRepository(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangInventoryRepository;
            case HCM -> hcmInventoryRepository;
            default -> hanoiInventoryRepository;
        };
    }
}
