package nexussMarket.infrastructure.security;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import nexussMarket.domain.ports.out.UserRepositoryPort;

/**
 * Loads domain users through {@link UserRepositoryPort} and wraps them in
 * {@link AuthenticatedUserPrincipal}. The Spring Security username is the
 * user's email; {@link #loadUserById(String)} serves the JWT filter, whose
 * tokens identify users by identifier.
 */
@Component
public class NexusUserDetailsService implements UserDetailsService {

    private final UserRepositoryPort userRepositoryPort;

    public NexusUserDetailsService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public AuthenticatedUserPrincipal loadUserByUsername(String email) {
        return userRepositoryPort.findByEmail(email)
                .map(AuthenticatedUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email " + email));
    }

    /** The user with {@code identifier}, or empty if it does not exist. */
    public Optional<AuthenticatedUserPrincipal> loadUserById(String identifier) {
        return userRepositoryPort.findById(identifier).map(AuthenticatedUserPrincipal::new);
    }
}
