package com.realestate.exception;

public class InquiryNotFoundException extends RuntimeException {

    public InquiryNotFoundException(Long inquiryId) {
        super("Inquiry not found: " + inquiryId);
    }
}
