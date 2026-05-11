package com.example.csdlpt.enums;

import lombok.Getter;

@Getter
public enum SiteCode {
    HN("Chi nhánh Hà Nội"),
    DN("Chi nhánh Đà Nẵng"),
    HCM("Chi nhánh Hồ Chí Minh"),
    ;

    private final String siteName;

    SiteCode(String siteName) {
        this.siteName = siteName;
    }
}
