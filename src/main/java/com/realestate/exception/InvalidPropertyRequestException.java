package com.realestate.exception;

public class InvalidPropertyRequestException extends RuntimeException {

    public InvalidPropertyRequestException(String message) {
        super(message);
    }
}
