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

    // Fallback local buckets — used only when Redis is unreachable
    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();

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
            return !localBuckets
                    .computeIfAbsent(ip, k -> buildLocalBucket())
                    .tryConsume(1);
        }
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
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
