package com.pragma.demo.services;

public class ReplayException extends RuntimeException {
    public ReplayException(String reason) {
        super(reason);
    }
}
