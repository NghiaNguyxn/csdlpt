package com.example.csdlpt.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

        log.error("Loi chua xu ly:", ex);
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
                        .message("Body JSON khong hop le hoac dang rong")
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
}
