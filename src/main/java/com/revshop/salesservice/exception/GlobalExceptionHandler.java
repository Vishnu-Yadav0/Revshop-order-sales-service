package com.revshop.salesservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<Map<String, String>> handleFeignException(feign.FeignException ex) {
        Map<String, String> error = new HashMap<>();
        // Fallback message
        String msg = "Error from external service";
        try {
            // Try to parse the ApiResponse error message from down stream service if available
            String content = ex.contentUTF8();
            if (content != null && !content.isEmpty()) {
                // simple substring extraction to avoid org.json import if not present
                if (content.contains("\"message\"")) {
                     int start = content.indexOf("\"message\":\"") + 11;
                     int end = content.indexOf("\"", start);
                     if (start > 10 && end > start) {
                         msg = content.substring(start, end);
                     } else {
                         msg = content;
                     }
                } else {
                     msg = content;
                }
            } else {
                msg = ex.getMessage();
            }
        } catch (Exception e) {
            msg = ex.getMessage();
        }
        error.put("message", msg);
        return new ResponseEntity<>(error, HttpStatus.valueOf(ex.status() == 0 ? 500 : ex.status()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "An unexpected error occurred: " + ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
