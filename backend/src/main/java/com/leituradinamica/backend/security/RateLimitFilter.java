package com.leituradinamica.backend.security;

import com.leituradinamica.backend.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final AppProperties appProperties;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getRemoteAddr();
        Counter counter = counters.compute(key, (ignored, current) -> current == null || current.expiresAt().isBefore(Instant.now())
                ? new Counter(1, Instant.now().plusSeconds(60))
                : new Counter(current.count() + 1, current.expiresAt()));

        if (counter.count() > appProperties.rateLimit().requestsPerMinute()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"message\":\"Rate limit excedido.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private record Counter(int count, Instant expiresAt) {
    }
}