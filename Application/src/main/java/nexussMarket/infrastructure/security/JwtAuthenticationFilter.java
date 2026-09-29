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
import nexussMarket.domain.services.AuditOperationService;

/**
 * Authenticates requests carrying {@code Authorization: Bearer <jwt>}. A
 * valid token whose user still exists and is {@code ACTIVE} puts an
 * {@link AuthenticatedUserPrincipal} in the {@link SecurityContext}, and the
 * request is recorded through {@link AuditOperationService} as
 * {@code "<METHOD> <path>"}.
 *
 * <p>Requests without a valid token continue unauthenticated; the security
 * configuration then rejects them on protected routes. The principal is
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
    private final AuditOperationService auditOperationService;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, NexusUserDetailsService userDetailsService,
                                   AuditOperationService auditOperationService) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
        this.auditOperationService = auditOperationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        bearerToken(request)
                .flatMap(jwtProvider::validate)
                .flatMap(claims -> userDetailsService.loadUserById(claims.userId()))
                .filter(principal -> principal.isEnabled() && principal.isAccountNonLocked())
                .ifPresent(principal -> authenticate(principal, request));
        filterChain.doFilter(request, response);
    }

    private void authenticate(AuthenticatedUserPrincipal principal, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // The query string is left out on purpose: it may carry sensitive values.
        auditOperationService.record(principal.getUserId(), request.getMethod() + " " + request.getRequestURI());
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
