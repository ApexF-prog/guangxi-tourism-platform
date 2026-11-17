package com.gxtourism.common;

/** 业务异常：GlobalExceptionHandler 统一转成 code=400 的 Result */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
