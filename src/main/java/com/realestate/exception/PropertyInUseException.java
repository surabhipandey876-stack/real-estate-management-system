package com.realestate.exception;

public class PropertyInUseException extends RuntimeException {

    public PropertyInUseException(Long id, Throwable cause) {
        super("Property " + id + " cannot be deleted because it is referenced by other records.", cause);
    }
}
