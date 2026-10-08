package com.realestate.exception;

public class VisitAccessDeniedException extends RuntimeException {

    public VisitAccessDeniedException() {
        super("Only customers can schedule or cancel property visits.");
    }

    public VisitAccessDeniedException(String message) {
        super(message);
    }
}
