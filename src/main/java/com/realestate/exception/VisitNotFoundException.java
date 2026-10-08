package com.realestate.exception;

public class VisitNotFoundException extends RuntimeException {

    public VisitNotFoundException(Long id) {
        super("Property visit not found: " + id);
    }
}
