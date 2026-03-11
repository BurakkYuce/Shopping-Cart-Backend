package com.burak.dream_shops.exceptions;

public class AlreadyExistsException extends  RuntimeException{
    public AlreadyExistsException(String message) {
        super(message);
    }
}
