package com.example.csdlpt.interceptor;

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
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiteInterceptor implements HandlerInterceptor {

    private final HanoiCustomerIdentityRepository hanoiCustomerRepository;
    private final DanangCustomerIdentityRepository danangCustomerRepository;
    private final HcmCustomerIdentityRepository hcmCustomerRepository;

    private static final String EMAIL_HEADER = "User-Email";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (shouldSkipProductWrite(request)) {
            return true;
        }

        String email = request.getHeader(EMAIL_HEADER);

        if (email == null || email.isBlank()) {
            // Optional: If no email, default to Hanoi or throw an error. Let's force an
            // error for strict demo.
            log.warn("Missing User-Email header");
            throw new AppException(ErrorCode.INVALID_KEY, "Thiếu header User-Email để xác định chi nhánh");
        }

        log.info("Bắt đầu tra cứu bản sao customer_identity theo email: {}", email);

        // Replicated identity lookup: customer_identity is copied at every site so any
        // replica can determine the customer's main site. Keep replica fallback for the
        // demo to tolerate stale/unavailable seed data during local experiments.
        CustomerIdentity customer = hanoiCustomerRepository.findByEmail(email)
                .orElseGet(() -> danangCustomerRepository.findByEmail(email)
                        .orElseGet(() -> hcmCustomerRepository.findByEmail(email)
                                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY,
                                        "Không tìm thấy khách hàng ở bất kỳ chi nhánh nào"))));

        String siteCode = customer.getMainSite().getSiteCode();
        log.info("Đã tìm thấy khách hàng ở chi nhánh: {}", siteCode);

        // Set context (Chuyển String sang Enum an toàn)
        try {
            SiteContextHolder.setCurrentSite(SiteCode.valueOf(siteCode.toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.error("SiteCode không hợp lệ từ DB: {}", siteCode);
            throw new AppException(ErrorCode.INVALID_KEY, "Chi nhánh không hợp lệ trong hệ thống");
        }

        return true; // Continue to Controller
    }

    private boolean shouldSkipProductWrite(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/products")
                && !"GET".equalsIgnoreCase(request.getMethod());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // Prevent memory leaks by clearing the ThreadLocal variable
        SiteContextHolder.clear();
    }
}
