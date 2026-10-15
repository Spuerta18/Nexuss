package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.AuthenticationResult;
import nexussMarket.domain.models.Buyer;

/** Operations available without an authenticated user. */
public interface PublicAccessPort {

    AuthenticationResult authenticate(String email, String password);

    Buyer registerBuyer(Buyer buyer, String password);
}
