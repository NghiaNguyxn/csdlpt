package com.example.csdlpt.service.Customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.csdlpt.repository.site_dn.DanangCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hcm.HcmCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hn.HanoiCustomerIdentityRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Ghi bản sao customer_identity bằng transaction manager của site đích.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerIdentityReplicationWriter {

    HanoiCustomerIdentityRepository hanoiIdentityRepo;
    DanangCustomerIdentityRepository danangIdentityRepo;
    HcmCustomerIdentityRepository hcmIdentityRepo;

    @Transactional("hanoiTransactionManager")
    public void replicateToHanoi(Long id, String email, String password, Integer mainSiteId) {
        hanoiIdentityRepo.replicateCustomerIdentity(id, email, password, mainSiteId);
    }

    @Transactional("danangTransactionManager")
    public void replicateToDanang(Long id, String email, String password, Integer mainSiteId) {
        danangIdentityRepo.replicateCustomerIdentity(id, email, password, mainSiteId);
    }

    @Transactional("hcmTransactionManager")
    public void replicateToHcm(Long id, String email, String password, Integer mainSiteId) {
        hcmIdentityRepo.replicateCustomerIdentity(id, email, password, mainSiteId);
    }
}
