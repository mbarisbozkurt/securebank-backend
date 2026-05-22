package com.securebank.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.dto.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int windowSeconds;
    private final int generalLimit;
    private final int sensitiveLimit;
    private final Map<String, RequestWindow> windows = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            ObjectMapper objectMapper,
            @Value("${rate-limit.enabled:true}") boolean enabled,
            @Value("${rate-limit.window-seconds:60}") int windowSeconds,
            @Value("${rate-limit.general-limit:600}") int generalLimit,
            @Value("${rate-limit.sensitive-limit:60}") int sensitiveLimit) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.windowSeconds = windowSeconds;
        this.generalLimit = generalLimit;
        this.sensitiveLimit = sensitiveLimit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String key = clientIp(request) + ":" + bucketName(request);
        int limit = isSensitiveEndpoint(request) ? sensitiveLimit : generalLimit;
        long now = Instant.now().getEpochSecond();

        RequestWindow window = windows.compute(key, (ignored, current) -> {
            if (current == null || now >= current.windowStartedAt + windowSeconds) {
                return new RequestWindow(now, 1);
            }
            return new RequestWindow(current.windowStartedAt, current.requestCount + 1);
        });

        if (window.requestCount > limit) {
            writeRateLimitResponse(response, request);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String bucketName(HttpServletRequest request) {
        return isSensitiveEndpoint(request) ? "sensitive" : "general";
    }

    private boolean isSensitiveEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/auth/") || uri.equals("/api/transfers");
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletResponse response, HttpServletRequest request) throws IOException {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        ApiErrorResponse body = ApiErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                "Too many requests. Please try again later.",
                request.getRequestURI()
        );

        response.setStatus(status.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), body);
    }

    private record RequestWindow(long windowStartedAt, int requestCount) {
    }
}
