package com.hmall.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends CommonException{

    public UnauthorizedException(String message) {
        super(message, 401);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause, 401);
    }

    public UnauthorizedException(Throwable cause) {
        super(cause, 401);
    }
}
