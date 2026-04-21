package com.example.csdlpt.context;

public class SiteContextHolder {

    private static final ThreadLocal<String> CURRENT_SITE = new ThreadLocal<>();

    public static void setCurrentSite(String siteCode) {
        CURRENT_SITE.set(siteCode);
    }

    public static String getCurrentSite() {
        return CURRENT_SITE.get();
    }

    public static void clear() {
        CURRENT_SITE.remove();
    }
}
