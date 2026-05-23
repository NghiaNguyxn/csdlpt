package com.example.csdlpt.service.Customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.csdlpt.dto.request.CustomerRequest;
import com.example.csdlpt.entity.CustomerProfile;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.site_dn.DanangCustomerProfileRepository;
import com.example.csdlpt.repository.site_hcm.HcmCustomerProfileRepository;
import com.example.csdlpt.repository.site_hn.HanoiCustomerProfileRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Ghi customer_profile trong cùng transaction với identity của đúng site.
 * Cách này tránh lỗi detached entity khi CustomerProfile dùng @MapsId.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerProfileWriter {

    HanoiCustomerProfileRepository hanoiProfileRepository;
    DanangCustomerProfileRepository danangProfileRepository;
    HcmCustomerProfileRepository hcmProfileRepository;

    @Transactional("hanoiTransactionManager")
    public CustomerProfile saveAtHanoi(Long id, CustomerRequest request) {
        hanoiProfileRepository.upsertProfile(id, request.getName(), request.getPhone(), request.getAddress());
        return hanoiProfileRepository.findById(id)
                .orElseThrow(() -> profileNotFound(id, "HN"));
    }

    @Transactional("danangTransactionManager")
    public CustomerProfile saveAtDanang(Long id, CustomerRequest request) {
        danangProfileRepository.upsertProfile(id, request.getName(), request.getPhone(), request.getAddress());
        return danangProfileRepository.findById(id)
                .orElseThrow(() -> profileNotFound(id, "DN"));
    }

    @Transactional("hcmTransactionManager")
    public CustomerProfile saveAtHcm(Long id, CustomerRequest request) {
        hcmProfileRepository.upsertProfile(id, request.getName(), request.getPhone(), request.getAddress());
        return hcmProfileRepository.findById(id)
                .orElseThrow(() -> profileNotFound(id, "HCM"));
    }

    private AppException profileNotFound(Long id, String siteCode) {
        return new AppException(ErrorCode.INVALID_KEY,
                "Không tìm thấy customer_profile ID=" + id + " tại " + siteCode);
    }
}
