package com.two_m.yourbarber.exception;

public record ErrorResponse(int status, String error, String message, long timestamp) {}
