package com.example.csdlpt.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    // ===== Lỗi hệ thống & Kết nối (9xxx) =====
    UNCATEGORIED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    SITE_CONNECTION_ERROR(9002, "Lỗi kết nối hoặc truy vấn giữa các chi nhánh", HttpStatus.INTERNAL_SERVER_ERROR),

    // ===== CRUD Sản phẩm & Danh mục (1xxx) =====
    PRODUCT_NOT_FOUND(1001, "Không tìm thấy sản phẩm", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(1002, "Danh mục không tồn tại", HttpStatus.NOT_FOUND),
    PRODUCT_EXISTED(1003, "Sản phẩm đã tồn tại trong hệ thống", HttpStatus.BAD_REQUEST),

    // ===== Kho hàng & Tồn kho (2xxx) =====
    INSUFFICIENT_STOCK(2001, "Số lượng hàng trong kho không đủ", HttpStatus.BAD_REQUEST),

    // ===== Thao tác DB (4xxx) =====
    CREATE_FAILED(4001, "Tạo mới dữ liệu thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    UPDATE_FAILED(4002, "Cập nhật dữ liệu thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    DELETE_FAILED(4003, "Xóa dữ liệu thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(4004, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
