package com.example.csdlpt.service.Customer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.example.csdlpt.dto.request.CustomerRequest;
import com.example.csdlpt.dto.response.CustomerResponse;
import com.example.csdlpt.entity.CustomerIdentity;
import com.example.csdlpt.entity.CustomerProfile;
import com.example.csdlpt.entity.Site;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.site_dn.DanangCustomerIdentityRepository;
import com.example.csdlpt.repository.site_dn.DanangCustomerProfileRepository;
import com.example.csdlpt.repository.site_hcm.HcmCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hcm.HcmCustomerProfileRepository;
import com.example.csdlpt.repository.site_hn.HanoiCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hn.HanoiCustomerProfileRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerService {

    HanoiCustomerIdentityRepository hanoiCustomerIdentityRepository;
    DanangCustomerIdentityRepository danangCustomerIdentityRepository;
    HcmCustomerIdentityRepository hcmCustomerIdentityRepository;
    HanoiCustomerProfileRepository hanoiCustomerProfileRepository;
    DanangCustomerProfileRepository danangCustomerProfileRepository;
    HcmCustomerProfileRepository hcmCustomerProfileRepository;
    CustomerIdentityCreationHelper creationHelper;
    CustomerIdGenerator customerIdGenerator;
    CustomerProfileWriter customerProfileWriter;
    CustomerDeleteWriter customerDeleteWriter;

    public List<CustomerResponse> findAllCustomers() {
        return Stream.of(
                hanoiCustomerProfileRepository.findAll().stream().map(profile -> toResponse(profile, "HN")),
                danangCustomerProfileRepository.findAll().stream().map(profile -> toResponse(profile, "DN")),
                hcmCustomerProfileRepository.findAll().stream().map(profile -> toResponse(profile, "HCM")))
                .flatMap(stream -> stream)
                .sorted(Comparator.comparing(CustomerResponse::getId))
                .toList();
    }

    public CustomerResponse findCustomerById(Long id) {
        // Thử lần lượt cả 3 site vì identity có thể chưa được replicate đủ (lazy protocol)
        CustomerIdentity identity = hanoiCustomerIdentityRepository.findById(id)
                .or(() -> danangCustomerIdentityRepository.findById(id))
                .or(() -> hcmCustomerIdentityRepository.findById(id))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Không tìm thấy khách hàng ID=" + id));

        Integer mainSiteId = identity.getMainSite().getId();
        CustomerProfile profile = findProfileBySite(id, mainSiteId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY,
                        "Không tìm thấy customer_profile ID=" + id + " tại fragment site=" + mainSiteId));

        return toResponse(profile, siteCode(mainSiteId));
    }

    /**
     * Tạo khách hàng mới tại site do mainSiteId quyết định (lazy distributed protocol).
     * Identity được ghi tại site nguồn + log replication sang 2 site còn lại (trong cùng transaction).
     * Profile được ghi tại site nguồn.
     * CustomerReplicationJob sẽ định kỳ đồng bộ identity sang 2 site còn lại.
     */
    public CustomerResponse createCustomer(CustomerRequest request) {
        validateRequest(request);

        Long customerId = customerIdGenerator.generate(request.getMainSiteId());
        ensureCustomerIdNotExistsAnywhere(customerId);

        CustomerIdentity identity = CustomerIdentity.builder()
                .id(customerId)
                .email(request.getEmail())
                .password(request.getPassword())
                .mainSite(Site.builder().id(request.getMainSiteId()).build())
                .build();

        CustomerProfile savedProfile = switch (request.getMainSiteId()) {
            case 1 -> creationHelper.createCustomerAtHanoi(identity, request);
            case 2 -> creationHelper.createCustomerAtDanang(identity, request);
            case 3 -> creationHelper.createCustomerAtHcm(identity, request);
            default -> throw new AppException(ErrorCode.INVALID_KEY, "mainSiteId không hợp lệ (1=HN, 2=DN, 3=HCM)");
        };

        log.info("Đã tạo customer ID={} tại site {}, replication log đã ghi cho 2 site còn lại",
                savedProfile.getId(), request.getMainSiteId());
        return toResponse(savedProfile, siteCode(request.getMainSiteId()));
    }

    
    // Cập nhật khách hàng — tra cứu identity tại site gốc (mainSiteId trong request).
    
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        validateRequest(request);

        CustomerIdentity identity = findIdentityBySite(id, request.getMainSiteId());
        Integer oldMainSiteId = identity.getMainSite().getId();
        identity.setEmail(request.getEmail());
        identity.setPassword(request.getPassword());
        identity.setMainSite(Site.builder().id(request.getMainSiteId()).build());

        CustomerIdentity savedIdentity = switch (request.getMainSiteId()) {
            case 1 -> creationHelper.updateAndLogAtHanoi(identity);
            case 2 -> creationHelper.updateAndLogAtDanang(identity);
            case 3 -> creationHelper.updateAndLogAtHcm(identity);
            default -> throw new AppException(ErrorCode.INVALID_KEY, "mainSiteId không hợp lệ");
        };

        if (!oldMainSiteId.equals(request.getMainSiteId())) {
            deleteProfileAtSite(id, oldMainSiteId);
        }
        CustomerProfile savedProfile = saveProfileAtSite(request, savedIdentity);

        log.info("Đã cập nhật customer ID={} tại site {}", id, request.getMainSiteId());
        return toResponse(savedProfile, siteCode(request.getMainSiteId()));
    }

    public void deleteCustomer(Long id) {
        // Tìm identity tại bất kỳ site nào để lấy mainSiteId
        CustomerIdentity identity = hanoiCustomerIdentityRepository.findById(id)
                .or(() -> danangCustomerIdentityRepository.findById(id))
                .or(() -> hcmCustomerIdentityRepository.findById(id))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Không tìm thấy khách hàng ID=" + id));

        deleteCustomerAndReplicas(id, identity.getMainSite().getId());

        log.info("Đã xóa customer ID={} khỏi fragment profile và các bản sao identity", id);
    }

    private void validateRequest(CustomerRequest request) {
        if (request.getEmail() == null || request.getPassword() == null
                || request.getMainSiteId() == null || request.getName() == null) {
            throw new AppException(ErrorCode.INVALID_KEY, "Thiếu thông tin bắt buộc của khách hàng");
        }
        if (request.getMainSiteId() < 1 || request.getMainSiteId() > 3) {
            throw new AppException(ErrorCode.INVALID_KEY, "mainSiteId chỉ nhận 1=HN, 2=DN, 3=HCM");
        }
    }

    private void ensureCustomerIdNotExistsAnywhere(Long id) {
        boolean exists = hanoiCustomerIdentityRepository.existsById(id)
                || danangCustomerIdentityRepository.existsById(id)
                || hcmCustomerIdentityRepository.existsById(id);
        if (exists) {
            throw new AppException(ErrorCode.INVALID_KEY,
                    "Khách hàng ID=" + id + " đã tồn tại ở ít nhất một site");
        }
    }

    private CustomerIdentity findIdentityBySite(Long id, Integer siteId) {
        return switch (siteId) {
            case 1 -> hanoiCustomerIdentityRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY,
                            "Không tìm thấy khách hàng ID=" + id + " tại HN"));
            case 2 -> danangCustomerIdentityRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY,
                            "Không tìm thấy khách hàng ID=" + id + " tại DN"));
            case 3 -> hcmCustomerIdentityRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY,
                            "Không tìm thấy khách hàng ID=" + id + " tại HCM"));
            default -> throw new AppException(ErrorCode.INVALID_KEY, "mainSiteId không hợp lệ");
        };
    }

    private Optional<CustomerProfile> findProfileBySite(Long id, Integer siteId) {
        return switch (siteId) {
            case 1 -> hanoiCustomerProfileRepository.findById(id);
            case 2 -> danangCustomerProfileRepository.findById(id);
            case 3 -> hcmCustomerProfileRepository.findById(id);
            default -> Optional.empty();
        };
    }

    private CustomerProfile saveProfileAtSite(CustomerRequest request, CustomerIdentity identity) {
        return switch (request.getMainSiteId()) {
            case 1 -> customerProfileWriter.saveAtHanoi(identity.getId(), request);
            case 2 -> customerProfileWriter.saveAtDanang(identity.getId(), request);
            case 3 -> customerProfileWriter.saveAtHcm(identity.getId(), request);
            default -> throw new AppException(ErrorCode.INVALID_KEY, "mainSiteId không hợp lệ");
        };
    }

    private void deleteProfileAtSite(Long id, Integer siteId) {
        switch (siteId) {
            case 1 -> customerDeleteWriter.deleteProfileAtHanoi(id);
            case 2 -> customerDeleteWriter.deleteProfileAtDanang(id);
            case 3 -> customerDeleteWriter.deleteProfileAtHcm(id);
            default -> throw new AppException(ErrorCode.INVALID_KEY, "mainSiteId không hợp lệ");
        }
    }

    private void deleteCustomerAndReplicas(Long id, Integer mainSiteId) {
        switch (mainSiteId) {
            case 1 -> {
                customerDeleteWriter.deleteCustomerAtHanoi(id);
                customerDeleteWriter.deleteIdentityAtDanang(id);
                customerDeleteWriter.deleteIdentityAtHcm(id);
            }
            case 2 -> {
                customerDeleteWriter.deleteIdentityAtHanoi(id);
                customerDeleteWriter.deleteCustomerAtDanang(id);
                customerDeleteWriter.deleteIdentityAtHcm(id);
            }
            case 3 -> {
                customerDeleteWriter.deleteIdentityAtHanoi(id);
                customerDeleteWriter.deleteIdentityAtDanang(id);
                customerDeleteWriter.deleteCustomerAtHcm(id);
            }
            default -> throw new AppException(ErrorCode.INVALID_KEY, "mainSiteId không hợp lệ");
        }
    }

    private CustomerResponse toResponse(CustomerProfile profile, String fragmentSiteCode) {
        CustomerIdentity identity = profile.getIdentity();
        Site mainSite = identity != null ? identity.getMainSite() : null;
        Integer mainSiteId = mainSite != null ? mainSite.getId() : null;

        return CustomerResponse.builder()
                .id(profile.getId())
                .email(identity != null ? identity.getEmail() : null)
                .mainSiteId(mainSiteId)
                .mainSiteCode(mainSiteId != null ? siteCode(mainSiteId) : null)
                .name(profile.getName())
                .phone(profile.getPhone())
                .address(profile.getAddress())
                .fragmentSiteCode(fragmentSiteCode)
                .build();
    }

    private String siteCode(Integer siteId) {
        return switch (siteId) {
            case 1 -> "HN";
            case 2 -> "DN";
            case 3 -> "HCM";
            default -> "UNKNOWN";
        };
    }
}
