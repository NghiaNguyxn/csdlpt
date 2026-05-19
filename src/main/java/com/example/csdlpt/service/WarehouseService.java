package com.example.csdlpt.service;

import com.example.csdlpt.dto.request.WarehouseRequest;
import com.example.csdlpt.dto.response.WarehouseResponse;
import com.example.csdlpt.entity.Site;
import com.example.csdlpt.entity.Warehouse;
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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WarehouseService {
    private final SiteRoutingService siteRoutingService;

    private final HanoiWarehouseRepository hanoiWarehouseRepository;
    private final DanangWarehouseRepository danangWarehouseRepository;
    private final HcmWarehouseRepository hcmWarehouseRepository;

    private final HanoiInventoryRepository hanoiInventoryRepository;
    private final DanangInventoryRepository danangInventoryRepository;
    private final HcmInventoryRepository hcmInventoryRepository;

    public List<WarehouseResponse> getWarehouses() {
        Map<Integer, WarehouseResponse> unique = new LinkedHashMap<>();

        for (SiteCode siteCode : SiteCode.values()) {
            for (Warehouse warehouse : siteRoutingService.findAllWareHouseBySite(siteCode)) {
                unique.putIfAbsent(warehouse.getId(), toResponse(warehouse, siteCode.name()));
            }
        }

        return new ArrayList<>(unique.values()).stream()
                .sorted(Comparator.comparing(WarehouseResponse::getId))
                .toList();
    }

    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        validateRequest(request);

        String code = request.getCode().trim();
        if (hanoiWarehouseRepository.existsByCodeIgnoreCase(code)
                || danangWarehouseRepository.existsByCodeIgnoreCase(code)
                || hcmWarehouseRepository.existsByCodeIgnoreCase(code)) {
            throw new AppException(ErrorCode.INVALID_KEY, "Mã kho đã tồn tại");
        }

        Integer siteId = resolveSiteId(request);
        String siteCode = resolveSiteCode(siteId);

        Warehouse warehouse = Warehouse.builder()
                .code(code)
                .name(request.getName().trim())
                .location(normalize(request.getLocation()))
                .region(normalize(request.getRegion()))
                .site(siteRef(siteId))
                .build();

        Warehouse savedMaster = hanoiWarehouseRepository.save(warehouse);

        saveOrUpdateReplicaInDanang(savedMaster, siteId);
        saveOrUpdateReplicaInHcm(savedMaster, siteId);

        return toResponse(savedMaster, "HN_MASTER_REPLICATED_" + siteCode);
    }

    public WarehouseResponse updateWarehouse(Integer id, WarehouseRequest request) {
        validateRequest(request);

        Warehouse master = hanoiWarehouseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        String newCode = request.getCode().trim();
        ensureCodeNotUsedByAnotherWarehouse(newCode, id);

        Integer siteId = resolveSiteId(request);
        String siteCode = resolveSiteCode(siteId);

        updateWarehouseData(master, request, siteId);
        Warehouse savedMaster = hanoiWarehouseRepository.save(master);

        saveOrUpdateReplicaInDanang(savedMaster, siteId);
        saveOrUpdateReplicaInHcm(savedMaster, siteId);

        return toResponse(savedMaster, "HN_MASTER_REPLICATED_" + siteCode);
    }

    public String deleteWarehouse(Integer id) {
        Warehouse master = hanoiWarehouseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        if (hanoiInventoryRepository.existsByWarehouseId(id)
                || danangInventoryRepository.existsByWarehouseId(id)
                || hcmInventoryRepository.existsByWarehouseId(id)) {
            throw new AppException(ErrorCode.DELETE_FAILED,
                    "Không thể xóa kho vì vẫn còn dữ liệu tồn kho liên quan");
        }

        hanoiWarehouseRepository.deleteById(id);

        if (danangWarehouseRepository.existsById(id)) {
            danangWarehouseRepository.deleteById(id);
        }

        if (hcmWarehouseRepository.existsById(id)) {
            hcmWarehouseRepository.deleteById(id);
        }

        return "Đã xóa kho " + master.getCode() + " ở master HN và các site nhân bản DN, HCM";
    }

    private void validateRequest(WarehouseRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_KEY, "Dữ liệu kho không được để trống");
        }

        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY, "Mã kho không được để trống");
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY, "Tên kho không được để trống");
        }

        resolveSiteId(request);
    }

    private void ensureCodeNotUsedByAnotherWarehouse(String code, Integer currentId) {
        hanoiWarehouseRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.INVALID_KEY, "Mã kho đã tồn tại ở kho khác");
                });

        danangWarehouseRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.INVALID_KEY, "Mã kho đã tồn tại ở kho khác");
                });

        hcmWarehouseRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.INVALID_KEY, "Mã kho đã tồn tại ở kho khác");
                });
    }

    private Integer resolveSiteId(WarehouseRequest request) {
        if (request.getSiteCode() != null && !request.getSiteCode().isBlank()) {
            String siteCode = request.getSiteCode().trim().toUpperCase();

            return switch (siteCode) {
                case "HN" -> 1;
                case "DN" -> 2;
                case "HCM" -> 3;
                default -> throw new AppException(ErrorCode.INVALID_KEY,
                        "siteCode chỉ được là HN, DN hoặc HCM");
            };
        }

        if (request.getSiteId() != null) {
            return switch (request.getSiteId()) {
                case 1 -> 1;
                case 2 -> 2;
                case 3 -> 3;
                default -> throw new AppException(ErrorCode.INVALID_KEY,
                        "siteId chỉ được là 1, 2 hoặc 3");
            };
        }

        throw new AppException(ErrorCode.INVALID_KEY, "Phải truyền siteCode hoặc siteId cho kho");
    }

    private String resolveSiteCode(Integer siteId) {
        if (siteId == null) {
            return null;
        }

        return switch (siteId) {
            case 1 -> "HN";
            case 2 -> "DN";
            case 3 -> "HCM";
            default -> null;
        };
    }

    private Site siteRef(Integer siteId) {
        return Site.builder()
                .id(siteId)
                .build();
    }

    private void updateWarehouseData(Warehouse warehouse, WarehouseRequest request, Integer siteId) {
        warehouse.setCode(request.getCode().trim());
        warehouse.setName(request.getName().trim());
        warehouse.setLocation(normalize(request.getLocation()));
        warehouse.setRegion(normalize(request.getRegion()));
        warehouse.setSite(siteRef(siteId));
    }

    private Warehouse copyWarehouse(Warehouse source, Integer siteId) {
        return Warehouse.builder()
                .id(source.getId())
                .code(source.getCode())
                .name(source.getName())
                .location(source.getLocation())
                .region(source.getRegion())
                .site(siteRef(siteId))
                .build();
    }

    private void saveOrUpdateReplicaInDanang(Warehouse source, Integer siteId) {
        danangWarehouseRepository.upsertWarehouse(
                source.getId(),
                source.getCode(),
                source.getName(),
                source.getLocation(),
                source.getRegion(),
                siteId
        );
    }

    private void saveOrUpdateReplicaInHcm(Warehouse source, Integer siteId) {
        hcmWarehouseRepository.upsertWarehouse(
                source.getId(),
                source.getCode(),
                source.getName(),
                source.getLocation(),
                source.getRegion(),
                siteId
        );
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private WarehouseResponse toResponse(Warehouse warehouse, String sourceSite) {
        Integer warehouseId = warehouse.getId();

        Integer siteId = warehouse.getSite() != null && warehouse.getSite().getId() != null
                ? warehouse.getSite().getId()
                : resolveSiteIdByWarehouseId(warehouseId);

        return WarehouseResponse.builder()
                .id(warehouseId)
                .code(warehouse.getCode())
                .name(warehouse.getName())
                .location(warehouse.getLocation())
                .region(warehouse.getRegion())
                .siteId(siteId)
                .siteCode(resolveSiteCode(siteId))
                .sourceSite(sourceSite)
                .build();
    }

    private Integer resolveSiteIdByWarehouseId(Integer warehouseId) {
        if (warehouseId == null) {
            return null;
        }

        return switch (warehouseId) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            default -> null;
        };
    }
}
