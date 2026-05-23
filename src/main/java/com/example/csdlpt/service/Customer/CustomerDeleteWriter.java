package com.example.csdlpt.service.Customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.csdlpt.repository.site_dn.DanangCustomerIdentityRepository;
import com.example.csdlpt.repository.site_dn.DanangCustomerProfileRepository;
import com.example.csdlpt.repository.site_hcm.HcmCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hcm.HcmCustomerProfileRepository;
import com.example.csdlpt.repository.site_hn.HanoiCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hn.HanoiCustomerProfileRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Xóa customer theo đúng transaction manager của từng site.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerDeleteWriter {

    HanoiCustomerIdentityRepository hanoiIdentityRepository;
    DanangCustomerIdentityRepository danangIdentityRepository;
    HcmCustomerIdentityRepository hcmIdentityRepository;
    HanoiCustomerProfileRepository hanoiProfileRepository;
    DanangCustomerProfileRepository danangProfileRepository;
    HcmCustomerProfileRepository hcmProfileRepository;

    @Transactional("hanoiTransactionManager")
    public void deleteCustomerAtHanoi(Long id) {
        deleteProfileAtHanoi(id);
        hanoiIdentityRepository.deleteReplicatedCustomerIdentity(id);
    }

    @Transactional("danangTransactionManager")
    public void deleteCustomerAtDanang(Long id) {
        deleteProfileAtDanang(id);
        danangIdentityRepository.deleteReplicatedCustomerIdentity(id);
    }

    @Transactional("hcmTransactionManager")
    public void deleteCustomerAtHcm(Long id) {
        deleteProfileAtHcm(id);
        hcmIdentityRepository.deleteReplicatedCustomerIdentity(id);
    }

    @Transactional("hanoiTransactionManager")
    public void deleteProfileAtHanoi(Long id) {
        hanoiProfileRepository.deleteById(id);
    }

    @Transactional("danangTransactionManager")
    public void deleteProfileAtDanang(Long id) {
        danangProfileRepository.deleteById(id);
    }

    @Transactional("hcmTransactionManager")
    public void deleteProfileAtHcm(Long id) {
        hcmProfileRepository.deleteById(id);
    }

    @Transactional("hanoiTransactionManager")
    public void deleteIdentityAtHanoi(Long id) {
        hanoiIdentityRepository.deleteReplicatedCustomerIdentity(id);
    }

    @Transactional("danangTransactionManager")
    public void deleteIdentityAtDanang(Long id) {
        danangIdentityRepository.deleteReplicatedCustomerIdentity(id);
    }

    @Transactional("hcmTransactionManager")
    public void deleteIdentityAtHcm(Long id) {
        hcmIdentityRepository.deleteReplicatedCustomerIdentity(id);
    }
}
