package nexussMarket.infrastructure.security;

import org.springframework.stereotype.Component;

import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.TokenServicePort;

/** Implements {@link TokenServicePort} by issuing JWTs through {@link JwtProvider}. */
@Component
public class JwtTokenServiceAdapter implements TokenServicePort {

    private final JwtProvider jwtProvider;

    public JwtTokenServiceAdapter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public String generateToken(User user) {
        return jwtProvider.generateToken(user);
    }
}
