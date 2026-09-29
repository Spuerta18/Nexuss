package nexussMarket.infrastructure.security;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import nexussMarket.domain.models.User;

/**
 * Issues and validates the HS256-signed JWTs used to authenticate requests.
 * Tokens carry the user identifier as {@code sub}, the {@code SystemRole}
 * code as {@code role}, and the {@code iat} / {@code exp} timestamps.
 */
@Component
public class JwtProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);
    private static final String ROLE_CLAIM = "role";

    private final SecretKey signingKey;
    private final JwtProperties properties;

    public JwtProvider(JwtProperties properties) {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException(
                    "nexus.security.jwt.secret is not set: define the NEXUS_JWT_SECRET environment variable");
        }
        // Keys.hmacShaKeyFor rejects keys shorter than 256 bits.
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.properties = properties;
    }

    /** A signed token identifying {@code user}, valid for the configured expiration. */
    public String generateToken(User user) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(user.getIdentifier())
                .claim(ROLE_CLAIM, user.getRole().getCode())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(properties.expiration())))
                .signWith(signingKey)
                .compact();
    }

    /**
     * The claims of {@code token}, or empty if it is malformed, not signed with
     * our key, or expired.
     */
    public Optional<JwtClaims> validate(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
            if (claims.getSubject() == null || claims.getExpiration() == null) {
                log.debug("Rejected JWT without subject or expiration");
                return Optional.empty();
            }
            return Optional.of(new JwtClaims(claims.getSubject(), claims.get(ROLE_CLAIM, String.class),
                    toInstant(claims.getIssuedAt()), claims.getExpiration().toInstant()));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static Instant toInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }

    /** Validated content of a token. */
    public record JwtClaims(String userId, String role, Instant issuedAt, Instant expiresAt) {
    }
}
