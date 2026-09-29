package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Buyer;

public interface RegisterBuyerUseCase {

    record Command(String identifier, String fullName, String email, String primaryAddress, String password) {}

    Buyer execute(Command command);
}
