package com.example.csdlpt.interceptor;

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

        CustomerIdentity customer = hanoiCustomerRepository.findByEmail(email)
                .orElseGet(() -> danangCustomerRepository.findByEmail(email)
                        .orElseGet(() -> hcmCustomerRepository.findByEmail(email)
                                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY,
                                        "Không tìm thấy khách hàng ở bất kỳ chi nhánh nào"))));

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

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SiteContextHolder.clear();
    }
}
