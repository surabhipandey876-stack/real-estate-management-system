package com.realestate.exception;

public class VisitStatusConflictException extends RuntimeException {

    public VisitStatusConflictException(Long visitId) {
        super("Only pending property visits can be accepted or rejected: " + visitId);
    }
}
