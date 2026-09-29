package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.valueobjects.SellerStatus;

public interface UpdateSellerStatusUseCase {

    record Command(String sellerId, SellerStatus status) {}

    Seller execute(User actor, Command command);
}
