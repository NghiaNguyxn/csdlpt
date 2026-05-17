package com.example.csdlpt.service;

import com.example.csdlpt.dto.request.CustomerIdentityRequest;
import com.example.csdlpt.dto.response.CustomerIdentityResponse;
import com.example.csdlpt.entity.CustomerIdentity;
import com.example.csdlpt.entity.Site;
import com.example.csdlpt.enums.ReplicationAction;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.site_hn.HanoiCustomerIdentityRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerIdentityService {

    HanoiCustomerIdentityRepository hanoiCustomerIdentityRepository;
    ReplicationService replicationService;

    @Transactional
    public CustomerIdentityResponse createCustomerIdentity(CustomerIdentityRequest request) {
        if (hanoiCustomerIdentityRepository.existsById(request.getId())) {
            throw new AppException(ErrorCode.INVALID_KEY, "CustomerIdentity đã tồn tại tại HN master");
        }

        CustomerIdentity customerIdentity = CustomerIdentity.builder()
                .id(request.getId())
                .email(request.getEmail())
                .password(request.getPassword())
                .mainSite(Site.builder().id(request.getMainSiteId()).build())
                .build();

        CustomerIdentity saved = hanoiCustomerIdentityRepository.save(customerIdentity);
        replicationService.logChange(saved.getId(), "CUSTOMER_IDENTITY", ReplicationAction.INSERT);

        log.info("Đã tạo CustomerIdentity tại HN master và ghi log lazy replication: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public CustomerIdentityResponse updateCustomerIdentity(Long id, CustomerIdentityRequest request) {
        CustomerIdentity customerIdentity = hanoiCustomerIdentityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY,
                        "Không tìm thấy CustomerIdentity ID=" + id + " tại HN master"));

        customerIdentity.setEmail(request.getEmail());
        customerIdentity.setPassword(request.getPassword());
        customerIdentity.setMainSite(Site.builder().id(request.getMainSiteId()).build());

        CustomerIdentity saved = hanoiCustomerIdentityRepository.save(customerIdentity);
        replicationService.logChange(saved.getId(), "CUSTOMER_IDENTITY", ReplicationAction.UPDATE);

        log.info("Đã cập nhật CustomerIdentity tại HN master và ghi log lazy replication: {}", saved.getId());
        return toResponse(saved);
    }

    private CustomerIdentityResponse toResponse(CustomerIdentity customerIdentity) {
        Site mainSite = customerIdentity.getMainSite();

        return CustomerIdentityResponse.builder()
                .id(customerIdentity.getId())
                .email(customerIdentity.getEmail())
                .mainSiteId(mainSite != null ? mainSite.getId() : null)
                .mainSiteCode(mainSite != null ? mainSite.getSiteCode() : null)
                .build();
    }

}
