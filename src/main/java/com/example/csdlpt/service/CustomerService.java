package com.example.csdlpt.service;

import com.example.csdlpt.dto.request.CustomerRequest;
import com.example.csdlpt.dto.response.CustomerResponse;
import com.example.csdlpt.entity.CustomerIdentity;
import com.example.csdlpt.entity.CustomerProfile;
import com.example.csdlpt.entity.Site;
import com.example.csdlpt.enums.ReplicationAction;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
    ReplicationService replicationService;

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
        CustomerIdentity identity = hanoiCustomerIdentityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Không tìm thấy khách hàng ID=" + id));

        Integer mainSiteId = identity.getMainSite().getId();
        CustomerProfile profile = findProfileBySite(id, mainSiteId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY,
                        "Không tìm thấy customer_profile ID=" + id + " tại fragment site=" + mainSiteId));

        return toResponse(profile, siteCode(mainSiteId));
    }

    @Transactional(value = "hanoiTransactionManager")
    public CustomerResponse createCustomer(CustomerRequest request) {
        validateRequest(request);
        if (hanoiCustomerIdentityRepository.existsById(request.getId())) {
            throw new AppException(ErrorCode.INVALID_KEY, "Khách hàng đã tồn tại tại HN master");
        }

        CustomerIdentity identity = CustomerIdentity.builder()
                .id(request.getId())
                .email(request.getEmail())
                .password(request.getPassword())
                .mainSite(Site.builder().id(request.getMainSiteId()).build())
                .build();

        CustomerIdentity savedIdentity = hanoiCustomerIdentityRepository.save(identity);
        ensureIdentityAtFragmentSite(savedIdentity);
        CustomerProfile savedProfile = saveProfileAtSite(request, savedIdentity);
        replicationService.logChange(savedIdentity.getId(), "CUSTOMER_IDENTITY", ReplicationAction.INSERT);

        log.info("Đã tạo customer ID={} với profile tại site {}", savedIdentity.getId(), request.getMainSiteId());
        return toResponse(savedProfile, siteCode(request.getMainSiteId()));
    }

    @Transactional(value = "hanoiTransactionManager")
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        validateRequest(request);
        CustomerIdentity identity = hanoiCustomerIdentityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Không tìm thấy khách hàng ID=" + id));

        Integer oldMainSiteId = identity.getMainSite().getId();
        identity.setEmail(request.getEmail());
        identity.setPassword(request.getPassword());
        identity.setMainSite(Site.builder().id(request.getMainSiteId()).build());

        CustomerIdentity savedIdentity = hanoiCustomerIdentityRepository.save(identity);
        ensureIdentityAtFragmentSite(savedIdentity);
        if (!oldMainSiteId.equals(request.getMainSiteId())) {
            deleteProfileAtSite(id, oldMainSiteId);
        }
        CustomerProfile savedProfile = saveProfileAtSite(request, savedIdentity);
        replicationService.logChange(savedIdentity.getId(), "CUSTOMER_IDENTITY", ReplicationAction.UPDATE);

        log.info("Đã cập nhật customer ID={} và route profile sang site {}", id, request.getMainSiteId());
        return toResponse(savedProfile, siteCode(request.getMainSiteId()));
    }

    @Transactional(value = "hanoiTransactionManager")
    public void deleteCustomer(Long id) {
        CustomerIdentity identity = hanoiCustomerIdentityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Không tìm thấy khách hàng ID=" + id));

        deleteProfileAtSite(id, identity.getMainSite().getId());
        hanoiCustomerIdentityRepository.deleteById(id);
        danangCustomerIdentityRepository.deleteReplicatedCustomerIdentity(id);
        hcmCustomerIdentityRepository.deleteReplicatedCustomerIdentity(id);

        log.info("Đã xóa customer ID={} khỏi fragment profile và các bản sao identity", id);
    }

    private void validateRequest(CustomerRequest request) {
        if (request.getId() == null || request.getEmail() == null || request.getPassword() == null
                || request.getMainSiteId() == null || request.getName() == null) {
            throw new AppException(ErrorCode.INVALID_KEY, "Thiếu thông tin bắt buộc của khách hàng");
        }
        if (request.getMainSiteId() < 1 || request.getMainSiteId() > 3) {
            throw new AppException(ErrorCode.INVALID_KEY, "mainSiteId chỉ nhận 1=HN, 2=DN, 3=HCM");
        }
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
        CustomerProfile profile = CustomerProfile.builder()
                .id(identity.getId())
                .identity(identity)
                .mainSite(Site.builder().id(request.getMainSiteId()).build())
                .name(request.getName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();

        return switch (request.getMainSiteId()) {
            case 1 -> hanoiCustomerProfileRepository.save(profile);
            case 2 -> danangCustomerProfileRepository.save(profile);
            case 3 -> hcmCustomerProfileRepository.save(profile);
            default -> throw new AppException(ErrorCode.INVALID_KEY, "mainSiteId không hợp lệ");
        };
    }

    private void deleteProfileAtSite(Long id, Integer siteId) {
        switch (siteId) {
            case 1 -> hanoiCustomerProfileRepository.deleteById(id);
            case 2 -> danangCustomerProfileRepository.deleteById(id);
            case 3 -> hcmCustomerProfileRepository.deleteById(id);
            default -> throw new AppException(ErrorCode.INVALID_KEY, "mainSiteId không hợp lệ");
        }
    }

    private void ensureIdentityAtFragmentSite(CustomerIdentity identity) {
        Integer mainSiteId = identity.getMainSite().getId();
        if (mainSiteId == 2) {
            danangCustomerIdentityRepository.replicateCustomerIdentity(
                    identity.getId(), identity.getEmail(), identity.getPassword(), mainSiteId);
        } else if (mainSiteId == 3) {
            hcmCustomerIdentityRepository.replicateCustomerIdentity(
                    identity.getId(), identity.getEmail(), identity.getPassword(), mainSiteId);
        }
    }

    private CustomerResponse toResponse(CustomerProfile profile, String fragmentSiteCode) {
        CustomerIdentity identity = profile.getIdentity();
        Site mainSite = profile.getMainSite();

        return CustomerResponse.builder()
                .id(profile.getId())
                .email(identity != null ? identity.getEmail() : null)
                .mainSiteId(mainSite != null ? mainSite.getId() : null)
                .mainSiteCode(mainSite != null ? mainSite.getSiteCode() : null)
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
