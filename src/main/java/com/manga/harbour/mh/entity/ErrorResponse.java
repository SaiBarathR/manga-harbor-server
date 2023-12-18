package com.manga.harbour.mh.entity;

import lombok.Getter;

@Getter
public class ErrorResponse {

    private String message;

    public ErrorResponse() {
    }

    public ErrorResponse(String message) {
        this.message = message;
    }

}
