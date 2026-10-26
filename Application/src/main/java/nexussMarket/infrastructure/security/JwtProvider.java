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
 * Issues and validates the HMAC-signed JWTs used to authenticate requests. The
 * algorithm follows the key length: HS256 for a 32-byte key, HS384 for 48 bytes,
 * HS512 for 64 bytes or more.
 * Tokens carry the user identifier as {@code sub}, the {@code SystemRole}
 * code as {@code role}, the {@code email}, the user's {@code authTokenVersion}
 * as {@code ver}, and the {@code iat} / {@code exp} timestamps. The
 * authentication filter reloads the user from the database and only accepts
 * a token whose {@code ver} is the user's current version (see
 * {@link JwtAuthenticationFilter}); a token without {@code ver} is rejected.
 */
@Component
public class JwtProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);
    private static final String ROLE_CLAIM = "role";
    private static final String EMAIL_CLAIM = "email";
    private static final String VERSION_CLAIM = "ver";

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
                .claim(EMAIL_CLAIM, user.getEmail())
                .claim(VERSION_CLAIM, user.getAuthTokenVersion())
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
            Integer version = claims.get(VERSION_CLAIM, Integer.class);
            if (claims.getSubject() == null || claims.getExpiration() == null || version == null) {
                log.debug("Rejected JWT without subject, expiration or version");
                return Optional.empty();
            }
            return Optional.of(new JwtClaims(claims.getSubject(), claims.get(ROLE_CLAIM, String.class),
                    claims.get(EMAIL_CLAIM, String.class), version, toInstant(claims.getIssuedAt()),
                    claims.getExpiration().toInstant()));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static Instant toInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }

    /** Validated content of a token. */
    public record JwtClaims(String userId, String role, String email, int version, Instant issuedAt,
                            Instant expiresAt) {
    }
}
