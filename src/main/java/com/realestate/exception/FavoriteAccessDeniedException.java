package com.realestate.exception;

public class FavoriteAccessDeniedException extends RuntimeException {

    public FavoriteAccessDeniedException() {
        super("Only customer accounts can manage favorites.");
    }
}
