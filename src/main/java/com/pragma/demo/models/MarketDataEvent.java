package com.pragma.demo.models;

public record MarketDataEvent(int id, long timestamp, String event, double price1, int shares1,
        String xchg1, double price2, int shares2, String xchg2) {
}
