package com.example.csdlpt.interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.csdlpt.context.SiteContextHolder;
import com.example.csdlpt.entity.CustomerIdentity;
import com.example.csdlpt.enums.SiteCode;
import com.example.csdlpt.exception.AppException;
import com.example.csdlpt.exception.ErrorCode;
import com.example.csdlpt.repository.site_dn.DanangCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hcm.HcmCustomerIdentityRepository;
import com.example.csdlpt.repository.site_hn.HanoiCustomerIdentityRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiteInterceptor implements HandlerInterceptor {

    private static final String EMAIL_HEADER = "User-Email";

    private final HanoiCustomerIdentityRepository hanoiCustomerRepository;
    private final DanangCustomerIdentityRepository danangCustomerRepository;
    private final HcmCustomerIdentityRepository hcmCustomerRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String email = request.getHeader(EMAIL_HEADER);

        if (email == null || email.isBlank()) {
            if (requiresUserEmail(request)) {
                log.warn("Thiếu header User-Email cho API riêng tư: {}", request.getRequestURI());
                throw new AppException(ErrorCode.INVALID_KEY, "Thiếu header User-Email để xác định chi nhánh");
            }

            return true;
        }

        log.info("Bắt đầu tra cứu bản sao customer_identity theo email: {}", email);

        List<SiteCode> unavailableSites = new ArrayList<>();
        CustomerIdentity customer = findCustomerIdentity(email, unavailableSites)
                .orElseThrow(() -> customerNotFound(email, unavailableSites));

        String siteCode = customer.getMainSite().getSiteCode();
        log.info("Đã tìm thấy khách hàng ở chi nhánh: {}", siteCode);

        try {
            SiteContextHolder.setCurrentSite(SiteCode.valueOf(siteCode.toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.error("SiteCode không hợp lệ từ DB: {}", siteCode);
            throw new AppException(ErrorCode.INVALID_KEY, "Chi nhánh không hợp lệ trong hệ thống");
        }

        return true;
    }

    private boolean requiresUserEmail(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/orders")
                || uri.startsWith("/api/inventories");
    }

    private Optional<CustomerIdentity> findCustomerIdentity(String email, List<SiteCode> unavailableSites) {
        return findCustomerIdentityAtSite(SiteCode.HN, () -> hanoiCustomerRepository.findByEmail(email), unavailableSites)
                .or(() -> findCustomerIdentityAtSite(SiteCode.DN,
                        () -> danangCustomerRepository.findByEmail(email), unavailableSites))
                .or(() -> findCustomerIdentityAtSite(SiteCode.HCM,
                        () -> hcmCustomerRepository.findByEmail(email), unavailableSites));
    }

    private Optional<CustomerIdentity> findCustomerIdentityAtSite(
            SiteCode siteCode,
            Supplier<Optional<CustomerIdentity>> lookup,
            List<SiteCode> unavailableSites) {
        try {
            return lookup.get();
        } catch (RuntimeException ex) {
            unavailableSites.add(siteCode);
            log.warn("Không thể tra cứu customer_identity tại site {}. Tiếp tục thử site khác. Lý do: {}",
                    siteCode, ex.getMessage());
            return Optional.empty();
        }
    }

    private AppException customerNotFound(String email, List<SiteCode> unavailableSites) {
        if (unavailableSites.size() == SiteCode.values().length) {
            return new AppException(ErrorCode.SITE_CONNECTION_ERROR,
                    "Không thể xác định chi nhánh của khách hàng vì tất cả site customer_identity đang không khả dụng");
        }
        if (!unavailableSites.isEmpty()) {
            return new AppException(ErrorCode.INVALID_KEY,
                    "Không tìm thấy khách hàng với email " + email
                            + " tại các chi nhánh đang khả dụng. Các site không truy vấn được: "
                            + unavailableSites);
        }
        return new AppException(ErrorCode.INVALID_KEY, "Không tìm thấy khách hàng ở bất kỳ chi nhánh nào");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SiteContextHolder.clear();
    }
}
