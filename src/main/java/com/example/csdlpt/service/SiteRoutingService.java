package com.example.csdlpt.service;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.example.csdlpt.entity.CustomerIdentity;
import com.example.csdlpt.entity.Inventory;
import com.example.csdlpt.entity.InventoryId;
import com.example.csdlpt.entity.ProductBasic;
import com.example.csdlpt.entity.Warehouse;
import com.example.csdlpt.enums.SiteCode;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.site_dn.DanangCustomerIdentityRepository;
import com.example.csdlpt.repository.site_dn.DanangInventoryRepository;
import com.example.csdlpt.repository.site_dn.DanangProductRepository;
import com.example.csdlpt.repository.site_dn.DanangWarehouseRepository;
import com.example.csdlpt.repository.site_hcm.HcmCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hcm.HcmInventoryRepository;
import com.example.csdlpt.repository.site_hcm.HcmProductRepository;
import com.example.csdlpt.repository.site_hcm.HcmWarehouseRepository;
import com.example.csdlpt.repository.site_hn.HanoiCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hn.HanoiInventoryRepository;
import com.example.csdlpt.repository.site_hn.HanoiProductRepository;
import com.example.csdlpt.repository.site_hn.HanoiWarehouseRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SiteRoutingService {

    // Hanoi Repositories
    HanoiProductRepository hanoiProductRepository;
    HanoiWarehouseRepository hanoiWarehouseRepository;
    HanoiInventoryRepository hanoiInventoryRepository;
    HanoiCustomerIdentityRepository hanoiCustomerIdentityRepository;

    // Danang Repositories
    DanangProductRepository danangProductRepository;
    DanangWarehouseRepository danangWarehouseRepository;
    DanangInventoryRepository danangInventoryRepository;
    DanangCustomerIdentityRepository danangCustomerIdentityRepository;

    // HCM Repositories
    HcmProductRepository hcmProductRepository;
    HcmWarehouseRepository hcmWarehouseRepository;
    HcmInventoryRepository hcmInventoryRepository;
    HcmCustomerIdentityRepository hcmCustomerIdentityRepository;

    // Tìm kiếm Product theo Site và ném lỗi nếu không tồn tại
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

    // Tìm kiếm Warehouse theo Site và ném lỗi nếu không tồn tại
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

    // Tìm kiếm tất cả các WareHouse theo Site
    public List<Warehouse> findAllWareHouseBySite(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangWarehouseRepository.findAll();
            case HCM -> hcmWarehouseRepository.findAll();
            default -> hanoiWarehouseRepository.findAll();
        };
    }

    public CustomerIdentity findCustomerBySite(Long customerId, SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangCustomerIdentityRepository.findById(customerId)
                    .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND, "Không tìm thấy khách hàng tại DN"));
            case HCM -> hcmCustomerIdentityRepository.findById(customerId)
                    .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND, "Không tìm thấy khách hàng tại HCM"));
            default -> hanoiCustomerIdentityRepository.findById(customerId)
                    .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND, "Không tìm thấy khách hàng tại HN"));
        };
    }

    public List<Inventory> findInventoryByProductAndSite(Integer productId, SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangInventoryRepository.findByProductId(productId);
            case HCM -> hcmInventoryRepository.findByProductId(productId);
            default -> hanoiInventoryRepository.findByProductId(productId);
        };
    }

    public List<Warehouse> findAllWareHouse() {
        return Stream.concat(danangWarehouseRepository.findAll().stream(),
                Stream.concat(hanoiWarehouseRepository.findAll().stream(),
                        hcmWarehouseRepository.findAll().stream()))
                .toList();
    }

    // Lấy Inventory Repository tương ứng với Site
    public JpaRepository<Inventory, InventoryId> getInventoryRepository(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangInventoryRepository;
            case HCM -> hcmInventoryRepository;
            default -> hanoiInventoryRepository;
        };
    }
}
