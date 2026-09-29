package nexussMarket.infrastructure.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * JWT settings, bound from {@code nexus.security.jwt.*}.
 *
 * @param secret     Base64-encoded HMAC key; at least 256 bits (32 bytes) for HS256.
 *                   Never committed: supplied through the {@code NEXUS_JWT_SECRET}
 *                   environment variable.
 * @param expiration how long an issued token stays valid.
 */
@ConfigurationProperties(prefix = "nexus.security.jwt")
public record JwtProperties(String secret, @DefaultValue("1h") Duration expiration) {
}
