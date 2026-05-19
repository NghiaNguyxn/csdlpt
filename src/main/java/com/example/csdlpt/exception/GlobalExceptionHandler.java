package com.example.csdlpt.exception;

import com.example.csdlpt.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<?>> handleException(Exception ex) {
        log.error("Unhandled Exception: ", ex);
        ApiResponse<?> response = new ApiResponse<>();

        response.setMessage(ErrorCode.UNCATEGORIED_EXCEPTION.getMessage());
        response.setCode(ErrorCode.UNCATEGORIED_EXCEPTION.getCode());

        return ResponseEntity.status(ErrorCode.UNCATEGORIED_EXCEPTION.getStatusCode())
                .body(response);
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
}
