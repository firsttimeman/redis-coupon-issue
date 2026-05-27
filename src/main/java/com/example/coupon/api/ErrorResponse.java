package com.example.coupon.api;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        LocalDateTime timestamp,
        String message,
        List<ValidationError> fieldErrors
) {

    public static ErrorResponse of(String message) {
        return new ErrorResponse(LocalDateTime.now(), message, List.of());
    }

    public static ErrorResponse of(String message, List<ValidationError> fieldErrors) {
        return new ErrorResponse(LocalDateTime.now(), message, fieldErrors);
    }

    public record ValidationError(
            String field,
            String message
    ) {
    }
}
