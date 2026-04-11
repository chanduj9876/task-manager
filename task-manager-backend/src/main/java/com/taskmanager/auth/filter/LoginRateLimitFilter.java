package com.taskmanager.auth.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter for POST /api/auth/login.
 *
 * Primary strategy: Redis INCR sliding window (distributed, survives restarts).
 * Fallback strategy: Bucket4j in-memory token bucket (used when Redis is unavailable).
 *
 * Limit: 5 attempts per minute per client IP.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String KEY_PREFIX = "rate:login:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate stringRedisTemplate;

    // Fallback local buckets with last-used timestamp to allow periodic eviction
    private record BucketEntry(Bucket bucket, Instant lastUsed) {}
    private final Map<String, BucketEntry> localBuckets = new ConcurrentHashMap<>();
    private static final int MAX_LOCAL_BUCKETS = 10_000;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && "/api/auth/login".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = resolveClientIp(request);

        if (isRateLimited(ip)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many login attempts. Please try again in 1 minute.\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String ip) {
        String key = KEY_PREFIX + ip;
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                stringRedisTemplate.expire(key, WINDOW);
            }
            return count != null && count > MAX_ATTEMPTS;
        } catch (Exception e) {
            log.warn("Redis unavailable for rate limiting, falling back to local Bucket4j: {}", e.getMessage());
            evictStaleLocalBuckets();
            BucketEntry entry = localBuckets.compute(ip, (k, existing) -> {
                Bucket bucket = existing != null ? existing.bucket() : buildLocalBucket();
                return new BucketEntry(bucket, Instant.now());
            });
            return !entry.bucket().tryConsume(1);
        }
    }

    private void evictStaleLocalBuckets() {
        if (localBuckets.size() < MAX_LOCAL_BUCKETS) return;
        Instant cutoff = Instant.now().minus(WINDOW.multipliedBy(2));
        localBuckets.entrySet().removeIf(e -> e.getValue().lastUsed().isBefore(cutoff));
    }

    /**
     * Bucket4j token-bucket used as fallback when Redis is down.
     * Allows MAX_ATTEMPTS requests, refilled fully every WINDOW duration.
     */
    private Bucket buildLocalBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(MAX_ATTEMPTS, Refill.intervally(MAX_ATTEMPTS, WINDOW)))
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        // Only trust X-Forwarded-For / X-Real-IP if explicitly configured to do so.
        // Blindly trusting these headers allows clients to spoof IPs and bypass rate limiting.
        // For production behind a trusted reverse proxy, set server.forward-headers-strategy=NATIVE
        // and Spring will handle IP resolution securely via RemoteAddrValueResolver.
        return request.getRemoteAddr();
    }
}
