package com.realestate.exception;

public class VisitCannotBeCancelledException extends RuntimeException {

    public VisitCannotBeCancelledException(Long id) {
        super("Property visit cannot be cancelled: " + id);
    }
}
