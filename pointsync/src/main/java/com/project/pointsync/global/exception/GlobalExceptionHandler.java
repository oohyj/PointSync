package com.project.pointsync.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 커스텀 에러의 경우
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        return ErrorResponse.fromException(e);
    }

    // 그 외 에러 내부 에러로 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {

        return ResponseEntity.internalServerError().body(
                ErrorResponse.builder()
                        .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                        .code("INTERNAL_SERVER_ERROR")
                        .message(e.getMessage())
                        .build()
        );
    }
}