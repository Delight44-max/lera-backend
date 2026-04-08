package com.lera.exception;

import org.springframework.http.HttpStatus;
import lombok.*;

@Getter
public class AppException extends RuntimeException {
    private final HttpStatus status;

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public static AppException notFound(String msg)    { return new AppException(msg, HttpStatus.NOT_FOUND); }
    public static AppException badRequest(String msg)  { return new AppException(msg, HttpStatus.BAD_REQUEST); }
    public static AppException forbidden(String msg)   { return new AppException(msg, HttpStatus.FORBIDDEN); }
    public static AppException conflict(String msg)    { return new AppException(msg, HttpStatus.CONFLICT); }
    public static AppException unauthorized(String msg){ return new AppException(msg, HttpStatus.UNAUTHORIZED); }
}
