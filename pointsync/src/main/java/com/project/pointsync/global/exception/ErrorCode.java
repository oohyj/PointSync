package com.project.pointsync.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

   //User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND , "해당 사용자를 찾을 수 없습니다."),
   USER_EMAIL_DUPLICATE(HttpStatus.BAD_REQUEST , "이미 가입된 이메일입니다.");

    private final HttpStatus status;
    private final String message;
}