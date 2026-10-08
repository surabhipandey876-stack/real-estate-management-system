package com.realestate.exception;

public class InquiryAccessDeniedException extends RuntimeException {

    public InquiryAccessDeniedException() {
        super("The authenticated user cannot access these inquiries.");
    }
}
