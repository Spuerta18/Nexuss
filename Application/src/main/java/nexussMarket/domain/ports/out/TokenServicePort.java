package nexussMarket.domain.ports.out;

import nexussMarket.domain.models.User;

public interface TokenServicePort {

    String generateToken(User user);
}
