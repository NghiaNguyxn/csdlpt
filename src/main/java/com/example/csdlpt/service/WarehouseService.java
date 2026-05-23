package com.example.csdlpt.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.csdlpt.context.SiteContextHolder;
import com.example.csdlpt.dto.request.WarehouseRequest;
import com.example.csdlpt.dto.response.WarehouseResponse;
import com.example.csdlpt.entity.Site;
import com.example.csdlpt.entity.Warehouse;
import com.example.csdlpt.enums.ReplicationAction;
import com.example.csdlpt.enums.SiteCode;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.site_dn.DanangInventoryRepository;
import com.example.csdlpt.repository.site_dn.DanangWarehouseRepository;
import com.example.csdlpt.repository.site_hcm.HcmInventoryRepository;
import com.example.csdlpt.repository.site_hcm.HcmWarehouseRepository;
import com.example.csdlpt.repository.site_hn.HanoiInventoryRepository;
import com.example.csdlpt.repository.site_hn.HanoiWarehouseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WarehouseService {
    private final HanoiWarehouseRepository hanoiWarehouseRepository;
    private final DanangWarehouseRepository danangWarehouseRepository;
    private final HcmWarehouseRepository hcmWarehouseRepository;

    private final HanoiInventoryRepository hanoiInventoryRepository;
    private final DanangInventoryRepository danangInventoryRepository;
    private final HcmInventoryRepository hcmInventoryRepository;

    private final ReplicationService replicationService;
    @Transactional(readOnly = true)
    public List<WarehouseResponse> getWarehouses() {
        SiteCode siteCode = currentSite();
        return findWarehousesBySite(siteCode).stream()
                .map(warehouse -> toResponse(warehouse, siteCode.name()))
                .sorted(Comparator.comparing(WarehouseResponse::getId))
                .toList();
    }

    @Transactional("hanoiTransactionManager")
    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        validateRequest(request);
        String code = request.getCode().trim();
        boolean existed = hanoiWarehouseRepository.findAll().stream()
                .anyMatch(warehouse -> warehouse.getCode() != null && warehouse.getCode().equalsIgnoreCase(code));
        if (existed) {
            throw new AppException(ErrorCode.INVALID_KEY, "Mã kho đã tồn tại ở master HN");
        }
        Integer siteId = resolveSiteId(request);
        Warehouse warehouse = Warehouse.builder()
                .code(code)
                .name(request.getName().trim())
                .location(normalize(request.getLocation()))
                .region(normalize(request.getRegion()))
                .site(siteRef(siteId))
                .build();
        Warehouse saved = hanoiWarehouseRepository.save(warehouse);
        replicationService.logChange(saved.getId().longValue(), "WAREHOUSE", ReplicationAction.INSERT);
        return toResponse(saved, "HN_MASTER_PENDING_REPLICATION");
    }

    @Transactional("hanoiTransactionManager")
    public WarehouseResponse updateWarehouse(Integer id, WarehouseRequest request) {
        validateRequest(request);
        Warehouse warehouse = hanoiWarehouseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        String code = request.getCode().trim();
        hanoiWarehouseRepository.findAll().stream()
                .filter(existing -> existing.getCode() != null && existing.getCode().equalsIgnoreCase(code))
                .filter(existing -> !existing.getId().equals(id))
                .findFirst()
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.INVALID_KEY, "Mã kho đã tồn tại ở kho khác tại master HN");
                });
        warehouse.setCode(code);
        warehouse.setName(request.getName().trim());
        warehouse.setLocation(normalize(request.getLocation()));
        warehouse.setRegion(normalize(request.getRegion()));
        warehouse.setSite(siteRef(resolveSiteId(request)));
        Warehouse saved = hanoiWarehouseRepository.save(warehouse);
        replicationService.logChange(saved.getId().longValue(), "WAREHOUSE", ReplicationAction.UPDATE);
        return toResponse(saved, "HN_MASTER_PENDING_REPLICATION");
    }

    @Transactional("hanoiTransactionManager")
    public String deleteWarehouse(Integer id) {
        Warehouse warehouse = hanoiWarehouseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        boolean used = hanoiInventoryRepository.findAll().stream().anyMatch(i -> id.equals(i.getId().getWarehouseId()))
                || danangInventoryRepository.findAll().stream().anyMatch(i -> id.equals(i.getId().getWarehouseId()))
                || hcmInventoryRepository.findAll().stream().anyMatch(i -> id.equals(i.getId().getWarehouseId()));
        if (used) {
            throw new AppException(ErrorCode.DELETE_FAILED, "Không thể xóa kho vì vẫn còn dữ liệu tồn kho liên quan");
        }
        hanoiWarehouseRepository.delete(warehouse);
        replicationService.logChange(id.longValue(), "WAREHOUSE", ReplicationAction.DELETE);
        return "Đã xóa kho ở master HN và ghi log lazy replication sang DN/HCM";
    }

    private void validateRequest(WarehouseRequest request) {
        if (request == null || request.getCode() == null || request.getCode().isBlank()
                || request.getName() == null || request.getName().isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY, "Mã kho và tên kho không được để trống");
        }
        resolveSiteId(request);
    }

    private Integer resolveSiteId(WarehouseRequest request) {
        if (request.getSiteId() != null) {
            if (request.getSiteId() < 1 || request.getSiteId() > 3) {
                throw new AppException(ErrorCode.INVALID_KEY, "siteId kho phai thuoc 1(HN), 2(DN), 3(HCM)");
            }
            return request.getSiteId();
        }
        if (request.getSiteCode() == null || request.getSiteCode().isBlank()) {
            return 1;
        }
        return switch (SiteCode.valueOf(request.getSiteCode().trim().toUpperCase())) {
            case DN -> 2;
            case HCM -> 3;
            default -> 1;
        };
    }

    private Site siteRef(Integer siteId) {
        String siteCode = siteId == 2 ? "DN" : siteId == 3 ? "HCM" : "HN";
        return Site.builder().id(siteId).siteCode(siteCode).build();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private WarehouseResponse toResponse(Warehouse warehouse, String sourceSite) {
        return WarehouseResponse.builder()
                .id(warehouse.getId())
                .code(warehouse.getCode())
                .name(warehouse.getName())
                .location(warehouse.getLocation())
                .region(warehouse.getRegion())
                .siteId(warehouse.getSite() == null ? null : warehouse.getSite().getId())
                .siteCode(warehouse.getSite() == null ? null : warehouse.getSite().getSiteCode())
                .sourceSite(sourceSite)
                .build();
    }

    private List<Warehouse> findWarehousesBySite(SiteCode siteCode) {
        return switch (siteCode) {
            case DN -> danangWarehouseRepository.findAll();
            case HCM -> hcmWarehouseRepository.findAll();
            default -> hanoiWarehouseRepository.findAll();
        };
    }

    private SiteCode currentSite() {
        SiteCode siteCode = SiteContextHolder.getCurrentSite();
        return siteCode == null ? SiteCode.HN : siteCode;
    }
}
