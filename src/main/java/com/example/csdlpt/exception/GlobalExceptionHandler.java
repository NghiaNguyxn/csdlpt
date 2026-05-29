package com.example.csdlpt.exception;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.csdlpt.dto.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<?>> handleException(Exception ex) {
        AppException appException = findAppException(ex);
        if (appException != null) {
            return handleAppException(appException);
        }

        log.error("Lỗi chưa xử lý:", ex);
        ApiResponse<?> response = new ApiResponse<>();
        response.setMessage(ErrorCode.UNCATEGORIED_EXCEPTION.getMessage());
        response.setCode(ErrorCode.UNCATEGORIED_EXCEPTION.getCode());

        return ResponseEntity.status(ErrorCode.UNCATEGORIED_EXCEPTION.getStatusCode())
                .body(response);
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(ErrorCode.INVALID_KEY.getStatusCode())
                .body(ApiResponse.builder()
                        .code(ErrorCode.INVALID_KEY.getCode())
                        .message("Body JSON không hợp lệ hoặc đang rỗng")
                        .build());
    }

    @ExceptionHandler(value = {
            CannotCreateTransactionException.class,
            CannotGetJdbcConnectionException.class,
            DataAccessResourceFailureException.class
    })
    ResponseEntity<ApiResponse<?>> handleSiteConnectionException(Exception ex) {
        String errorTrace = errorTrace(ex);
        String rootMessage = rootMessage(ex);
        String failedSite = inferFailedSite(errorTrace);
        String siteMessage = failedSite == null ? "chi nhánh" : "site " + failedSite;

        log.warn("Lỗi kết nối hoặc truy vấn datasource/site {}: {}", failedSite, rootMessage);

        return ResponseEntity
                .status(ErrorCode.SITE_CONNECTION_ERROR.getStatusCode())
                .body(ApiResponse.builder()
                        .code(ErrorCode.SITE_CONNECTION_ERROR.getCode())
                        .message("Không thể kết nối đến " + siteMessage
                                + ". Vui lòng thử lại sau.")
                        .build());
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<?>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : errorCode.getMessage();

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(message)
                        .build());
    }

    private AppException findAppException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof AppException appException) {
                return appException;
            }
            current = current.getCause();
        }
        return null;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        Throwable root = throwable;
        while (current != null) {
            root = current;
            current = current.getCause();
        }
        return root.getMessage();
    }

    private String errorTrace(Throwable throwable) {
        StringBuilder trace = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                trace.append(current.getMessage()).append('\n');
            }
            current = current.getCause();
        }
        return trace.toString();
    }

    private String inferFailedSite(String errorTrace) {
        if (errorTrace == null) {
            return null;
        }
        if (errorTrace.contains(":5431")) {
            return "HN";
        }
        if (errorTrace.contains(":5434")) {
            return "DN";
        }
        if (errorTrace.contains(":5433")) {
            return "HCM";
        }
        if (errorTrace.contains(":5435")) {
            return "CENTRAL";
        }
        return null;
    }
}
