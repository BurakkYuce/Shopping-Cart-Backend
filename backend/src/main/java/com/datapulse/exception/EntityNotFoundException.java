package com.datapulse.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entity, String id) {
        super(entity + " not found with id: " + id);
    }
}
