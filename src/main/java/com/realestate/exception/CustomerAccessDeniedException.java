package com.realestate.exception;

public class CustomerAccessDeniedException extends RuntimeException {

    public CustomerAccessDeniedException() {
        super("Only customers can access customer profile features.");
    }
}
