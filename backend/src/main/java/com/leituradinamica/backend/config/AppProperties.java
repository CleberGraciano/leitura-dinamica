package com.leituradinamica.backend.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Cors cors,
        RateLimit rateLimit
) {

    public record Jwt(
            String secret,
            long accessTokenExpirationMinutes,
            long refreshTokenExpirationDays
    ) {
    }

    public record Cors(String allowedOrigins) {
        public List<String> origins() {
            return Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList();
        }
    }

    public record RateLimit(int requestsPerMinute) {
    }
}