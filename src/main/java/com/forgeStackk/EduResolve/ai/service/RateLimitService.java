package com.forgeStackk.EduResolve.ai.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory per-user rate limiter.
 * 20 requests / minute per user key. Resets every minute via daemon thread.
 */
@Service
public class RateLimitService {

    private static final int MAX_PER_MINUTE = 20;

    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public RateLimitService() {
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-reset");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(counters::clear, 1, 1, TimeUnit.MINUTES);
    }

    /** Returns true when the request is within quota. */
    public boolean isAllowed(String key) {
        return counters.computeIfAbsent(key, k -> new AtomicInteger(0))
                       .incrementAndGet() <= MAX_PER_MINUTE;
    }
}
