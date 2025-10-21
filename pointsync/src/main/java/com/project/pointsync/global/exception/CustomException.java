package com.project.pointsync.global.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException{

    private final ErrorCode errorCode;

    private String info;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, String info){
        super(errorCode.getMessage() + " " + info);
        this.errorCode = errorCode;
        this.info = info;
    }
}