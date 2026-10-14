package com.minimarket.domain.exception;

public class ForbiddenOperationException extends BusinessException {

    public ForbiddenOperationException(String code, String message) {
        super(code, message);
    }
}
