package com.andre.ridematching.repository;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class RideIdGenerator {

    private final AtomicLong sequence = new AtomicLong();

    public long nextId() {
        return sequence.incrementAndGet();
    }
}
