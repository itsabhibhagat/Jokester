package com.application.jokester.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final LettuceBasedProxyManager<String> proxyManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Skip rate limiting for Swagger/OpenAPI docs and actuator
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);
        BucketConfiguration config = resolveBucketConfig(path, method);
        String bucketKey = "rate-limit:" + resolveBucketPrefix(path, method) + ":" + clientKey;

        Bucket bucket = proxyManager.builder().build(bucketKey, () -> config);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests. Please try again in "
                            + retryAfterSeconds + " seconds.\",\"timestamp\":\""
                            + java.time.LocalDateTime.now() + "\"}");
            log.warn("Rate limit exceeded for key: {}", bucketKey);
        }
    }

    /**
     * Resolves the client identifier. Uses IP for public endpoints,
     * or the authenticated user's info from the JWT token.
     */
    private String resolveClientKey(HttpServletRequest request) {
        // Try to get the real client IP behind proxies
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Selects the appropriate bucket configuration based on the endpoint.
     */
    private BucketConfiguration resolveBucketConfig(String path, String method) {
        if (path.startsWith("/api/auth/register")) {
            return RateLimitConfig.authBucketConfig();
        }
        if (path.startsWith("/api/auth/login")) {
            return RateLimitConfig.loginBucketConfig();
        }
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)) {
            return RateLimitConfig.writeBucketConfig();
        }
        return RateLimitConfig.readBucketConfig();
    }

    /**
     * Creates a prefix for the bucket key to separate rate limits per endpoint category.
     */
    private String resolveBucketPrefix(String path, String method) {
        if (path.startsWith("/api/auth/register")) return "auth-register";
        if (path.startsWith("/api/auth/login")) return "auth-login";
        if ("POST".equalsIgnoreCase(method)) return "write";
        if ("PUT".equalsIgnoreCase(method)) return "write";
        if ("DELETE".equalsIgnoreCase(method)) return "write";
        return "read";
    }
}
