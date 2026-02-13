package com.nocountry.api.service;

import com.nocountry.api.config.AppProperties;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class TrackRateLimiterService {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final int refillPerSecond;

    public TrackRateLimiterService(AppProperties appProperties) {
        this.capacity = Math.max(1, appProperties.getTracking().getRateLimitCapacity());
        this.refillPerSecond = Math.max(1, appProperties.getTracking().getRateLimitRefillPerSecond());
    }

    public boolean allow(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(capacity));
        synchronized (bucket) {
            long now = System.nanoTime();
            double elapsedSeconds = (now - bucket.lastRefillNanos) / 1_000_000_000.0;
            bucket.tokens = Math.min(capacity, bucket.tokens + (elapsedSeconds * refillPerSecond));
            bucket.lastRefillNanos = now;

            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                return true;
            }
            return false;
        }
    }

    private static final class Bucket {
        private double tokens;
        private long lastRefillNanos;

        private Bucket(int initialTokens) {
            this.tokens = initialTokens;
            this.lastRefillNanos = System.nanoTime();
        }
    }
}
