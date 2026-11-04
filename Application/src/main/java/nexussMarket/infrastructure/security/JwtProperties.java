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
 * @param generateIfMissing when {@code true} and no secret is set, a random key is generated at startup (the
 *                   {@code memory} profile does this, so it starts with one command); tokens then stop working
 *                   when the application restarts. Never used with a real database.
 */
@ConfigurationProperties(prefix = "nexus.security.jwt")
public record JwtProperties(String secret, @DefaultValue("1h") Duration expiration,
                            @DefaultValue("false") boolean generateIfMissing) {
}
