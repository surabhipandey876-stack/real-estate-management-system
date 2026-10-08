package com.realestate.exception;

public class PropertyAccessDeniedException extends RuntimeException {

    public PropertyAccessDeniedException() {
        super("You do not have permission to manage this property.");
    }
}
