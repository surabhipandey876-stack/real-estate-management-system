package com.realestate.exception;

public class PropertyImageStorageException extends RuntimeException {

    public PropertyImageStorageException(Throwable cause) {
        super("Property image storage is unavailable.", cause);
    }
}
