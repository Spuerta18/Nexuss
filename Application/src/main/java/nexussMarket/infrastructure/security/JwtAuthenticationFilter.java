package nexussMarket.infrastructure.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nexussMarket.infrastructure.security.JwtProvider.JwtClaims;

/**
 * Authenticates requests carrying {@code Authorization: Bearer <jwt>}. A
 * valid token whose user still exists, is {@code ACTIVE} (never
 * {@code BLOCKED}) and still has the role the token was issued for puts an
 * {@link AuthenticatedUserPrincipal} in the {@link SecurityContext}. The
 * principal wraps the reloaded domain user, so it is always the right
 * {@code User} subtype with its current state. Auditing is not done here:
 * each state-changing Domain Service records its own operation.
 *
 * <p>Requests without a valid token continue unauthenticated; the security
 * configuration then rejects them on protected routes with {@code 401}. The
 * token itself is never logged. The principal is
 * reloaded on every request, so blocking a user takes effect immediately and
 * authorities never come from the token alone.</p>
 *
 * <p>Not a Spring bean on purpose: as a bean, Spring Boot would also register
 * it as a servlet filter outside the security chain.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final NexusUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, NexusUserDetailsService userDetailsService) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        bearerToken(request)
                .flatMap(jwtProvider::validate)
                .flatMap(this::currentPrincipal)
                .ifPresent(principal -> authenticate(principal, request));
        filterChain.doFilter(request, response);
    }

    /**
     * The user the token was issued for, reloaded from the database, or empty
     * if it no longer exists, is not {@code ACTIVE}, or its stored role differs
     * from the role in the token.
     */
    private Optional<AuthenticatedUserPrincipal> currentPrincipal(JwtClaims claims) {
        return userDetailsService.loadUserById(claims.userId())
                .filter(principal -> principal.isEnabled() && principal.isAccountNonLocked())
                .filter(principal -> principal.getUser().getRole().getCode().equals(claims.role()));
    }

    private void authenticate(AuthenticatedUserPrincipal principal, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private static Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
