package nexussMarket.adapters.useCases;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import nexussMarket.domain.models.AuthenticationResult;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.ports.in.PublicAccessPort;
import nexussMarket.domain.services.AuthenticateUserService;
import nexussMarket.domain.services.RegisterBuyerService;

/** Implements {@link PublicAccessPort} by delegating each operation to its domain service. */
@Service
@RequiredArgsConstructor
public class PublicAccessUseCaseImpl implements PublicAccessPort {

    private final AuthenticateUserService authenticateUserService;
    private final RegisterBuyerService registerBuyerService;

    @Override
    public AuthenticationResult authenticate(String email, String password) {
        return authenticateUserService.execute(email, password);
    }

    @Override
    public Buyer registerBuyer(Buyer buyer, String password) {
        return registerBuyerService.execute(buyer, password);
    }
}
