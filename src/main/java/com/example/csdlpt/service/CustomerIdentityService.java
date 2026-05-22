package com.example.csdlpt.service;

import org.springframework.stereotype.Service;

import com.example.csdlpt.dto.request.CustomerIdentityRequest;
import com.example.csdlpt.dto.response.CustomerIdentityResponse;
import com.example.csdlpt.entity.CustomerIdentity;
import com.example.csdlpt.entity.Site;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.site_dn.DanangCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hcm.HcmCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hn.HanoiCustomerIdentityRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Quản lý CustomerIdentity với lazy distributed replication.
 *
 * Khác với product (chỉ tạo tại HN master), customer_identity có thể được tạo
 * tại bất kỳ site nào tùy theo mainSiteId trong request:
 *   - mainSiteId = 1 → tạo tại HN, ghi log replication sang DN + HCM
 *   - mainSiteId = 2 → tạo tại DN, ghi log replication sang HN + HCM
 *   - mainSiteId = 3 → tạo tại HCM, ghi log replication sang HN + DN
 *
 * Lazy protocol: CustomerReplicationJob định kỳ xử lý các log PENDING
 * và đẩy bản ghi tới các site đích, đảm bảo eventual consistency.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerIdentityService {

    HanoiCustomerIdentityRepository hanoiIdentityRepo;
    DanangCustomerIdentityRepository danangIdentityRepo;
    HcmCustomerIdentityRepository hcmIdentityRepo;
    CustomerIdentityCreationHelper creationHelper;
    CustomerIdGenerator customerIdGenerator;

    /**
     * Tạo customer_identity tại site do mainSiteId quyết định.
     * Ghi log replication ngay sau khi tạo (trong cùng transaction) để job sau xử lý lazy.
     */
    public CustomerIdentityResponse createCustomerIdentity(CustomerIdentityRequest request) {
        validateRequest(request);
        Long customerId = customerIdGenerator.generate(request.getMainSiteId());
        checkDuplicateAcrossSites(customerId);

        CustomerIdentity identity = buildIdentity(customerId, request);

        CustomerIdentity saved = switch (request.getMainSiteId()) {
            case 1 -> creationHelper.createAndLogAtHanoi(identity);
            case 2 -> creationHelper.createAndLogAtDanang(identity);
            case 3 -> creationHelper.createAndLogAtHcm(identity);
            default -> throw new AppException(ErrorCode.INVALID_KEY,
                    "mainSiteId không hợp lệ (1=HN, 2=DN, 3=HCM)");
        };

        return toResponse(saved);
    }

    /**
     * Cập nhật customer_identity tại site gốc của khách hàng (mainSiteId từ DB).
     * Ghi log replication để đồng bộ thay đổi sang các site còn lại.
     */
    public CustomerIdentityResponse updateCustomerIdentity(Long id, CustomerIdentityRequest request) {
        validateRequest(request);

        CustomerIdentity existing = findById(id, request.getMainSiteId());
        existing.setEmail(request.getEmail());
        existing.setPassword(request.getPassword());
        existing.setMainSite(Site.builder().id(request.getMainSiteId()).build());

        CustomerIdentity saved = switch (request.getMainSiteId()) {
            case 1 -> creationHelper.updateAndLogAtHanoi(existing);
            case 2 -> creationHelper.updateAndLogAtDanang(existing);
            case 3 -> creationHelper.updateAndLogAtHcm(existing);
            default -> throw new AppException(ErrorCode.INVALID_KEY, "mainSiteId không hợp lệ");
        };

        return toResponse(saved);
    }

    private void validateRequest(CustomerIdentityRequest request) {
        if (request.getEmail() == null || request.getPassword() == null || request.getMainSiteId() == null) {
            throw new AppException(ErrorCode.INVALID_KEY,
                    "Thiếu thông tin bắt buộc: email, password, mainSiteId");
        }
        if (request.getMainSiteId() < 1 || request.getMainSiteId() > 3) {
            throw new AppException(ErrorCode.INVALID_KEY, "mainSiteId chỉ nhận 1=HN, 2=DN, 3=HCM");
        }
    }

    /**
     * Kiểm tra trùng ID trên cả 3 site trước khi tạo.
     * Cách này giảm rủi ro trùng khóa trong mô hình lazy replication; để chống
     * race tuyệt đối vẫn cần bộ sinh ID toàn cục hoặc phân vùng ID theo site.
     */
    private void checkDuplicateAcrossSites(Long id) {
        boolean exists = hanoiIdentityRepo.existsById(id)
                || danangIdentityRepo.existsById(id)
                || hcmIdentityRepo.existsById(id);
        if (exists) {
            throw new AppException(ErrorCode.INVALID_KEY,
                    "CustomerIdentity id=" + id + " đã tồn tại ở ít nhất một site");
        }
    }

    private CustomerIdentity findById(Long id, Integer siteId) {
        return switch (siteId) {
            case 1 -> hanoiIdentityRepo.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY,
                            "Không tìm thấy identity id=" + id + " tại HN"));
            case 2 -> danangIdentityRepo.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY,
                            "Không tìm thấy identity id=" + id + " tại DN"));
            case 3 -> hcmIdentityRepo.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY,
                            "Không tìm thấy identity id=" + id + " tại HCM"));
            default -> throw new AppException(ErrorCode.INVALID_KEY, "mainSiteId không hợp lệ");
        };
    }

    private CustomerIdentity buildIdentity(Long customerId, CustomerIdentityRequest request) {
        return CustomerIdentity.builder()
                .id(customerId)
                .email(request.getEmail())
                .password(request.getPassword())
                .mainSite(Site.builder().id(request.getMainSiteId()).build())
                .build();
    }

    private CustomerIdentityResponse toResponse(CustomerIdentity identity) {
        Site mainSite = identity.getMainSite();
        return CustomerIdentityResponse.builder()
                .id(identity.getId())
                .email(identity.getEmail())
                .mainSiteId(mainSite != null ? mainSite.getId() : null)
                .mainSiteCode(mainSite != null ? mainSite.getSiteCode() : null)
                .build();
    }
}
