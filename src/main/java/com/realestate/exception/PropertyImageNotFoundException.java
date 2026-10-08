package com.realestate.exception;

public class PropertyImageNotFoundException extends RuntimeException {

    public PropertyImageNotFoundException(Long imageId, Long propertyId) {
        super("Image " + imageId + " was not found for property " + propertyId + ".");
    }
}
