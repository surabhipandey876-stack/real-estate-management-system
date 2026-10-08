package com.realestate.exception;

public class FavoriteAlreadyExistsException extends RuntimeException {

    public FavoriteAlreadyExistsException(Long propertyId) {
        super("Property is already in favorites: " + propertyId);
    }
}
