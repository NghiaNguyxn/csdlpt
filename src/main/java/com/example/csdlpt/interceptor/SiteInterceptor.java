package com.example.csdlpt.interceptor;

import com.example.csdlpt.context.SiteContextHolder;
import com.example.csdlpt.entity.CustomerIdentity;
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
        String email = request.getHeader(EMAIL_HEADER);

        if (email == null || email.isBlank()) {
            // Optional: If no email, default to Hanoi or throw an error. Let's force an
            // error for strict demo.
            log.warn("Missing User-Email header");
            throw new AppException(ErrorCode.INVALID_KEY, "Thiếu header User-Email để xác định chi nhánh");
        }

        log.info("Bắt đầu truy vấn phân tán để tìm khách hàng có email: {}", email);

        // Distributed Search Mechanism (Fragmentation Transparency)
        CustomerIdentity customer = hanoiCustomerRepository.findByEmail(email)
                .orElseGet(() -> danangCustomerRepository.findByEmail(email)
                        .orElseGet(() -> hcmCustomerRepository.findByEmail(email)
                                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY,
                                        "Không tìm thấy khách hàng ở bất kỳ chi nhánh nào"))));

        String siteCode = customer.getMainSite().getSiteCode();
        log.info("Đã tìm thấy khách hàng ở chi nhánh: {}", siteCode);

        // Set context
        SiteContextHolder.setCurrentSite(siteCode);

        return true; // Continue to Controller
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // Prevent memory leaks by clearing the ThreadLocal variable
        SiteContextHolder.clear();
    }
}
