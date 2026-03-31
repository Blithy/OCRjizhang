package com.example.ocrjizhang.utils

import java.util.concurrent.atomic.AtomicLong

object LocalIdGenerator {
    private val counter = AtomicLong(System.currentTimeMillis() * 100)

    fun nextId(): Long = counter.incrementAndGet()
}
