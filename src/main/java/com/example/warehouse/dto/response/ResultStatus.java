package com.example.warehouse.dto.response;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ResultStatus {
    SUCCESS("SUCCESS"),
    FAILED("FAILED");

    private final String value;

    ResultStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
