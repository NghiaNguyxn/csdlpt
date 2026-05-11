package com.example.csdlpt.context;

import com.example.csdlpt.enums.SiteCode;

public class SiteContextHolder {

    private static final ThreadLocal<SiteCode> CURRENT_SITE = new ThreadLocal<>();

    public static void setCurrentSite(SiteCode siteCode) {
        CURRENT_SITE.set(siteCode);
    }

    public static SiteCode getCurrentSite() {
        return CURRENT_SITE.get();
    }

    public static void clear() {
        CURRENT_SITE.remove();
    }
}
