package com.datapulse.dto.response;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class ApiErrorResponse {
    private String timestamp = Instant.now().toString();
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> fieldErrors;

    public ApiErrorResponse(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
}
