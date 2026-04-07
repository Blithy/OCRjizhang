package com.example.ocrjizhang.backend.manage;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class ManageIdGenerator {

    private final AtomicLong counter = new AtomicLong(System.currentTimeMillis());

    public long nextId() {
        return counter.incrementAndGet();
    }
}
