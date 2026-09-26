package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Seller;

public interface RegisterSellerUseCase {

    record Command(String identifier, String fullName, String email, String administratorId) {}

    Seller execute(Command command);
}
