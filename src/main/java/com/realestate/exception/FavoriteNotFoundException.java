package com.realestate.exception;

public class FavoriteNotFoundException extends RuntimeException {

    public FavoriteNotFoundException(Long propertyId) {
        super("Favorite property not found: " + propertyId);
    }
}
