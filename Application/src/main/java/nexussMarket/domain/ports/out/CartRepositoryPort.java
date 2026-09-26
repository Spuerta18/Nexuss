package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.models.ShoppingCart;

public interface CartRepositoryPort {

    ShoppingCart save(ShoppingCart cart);

    Optional<ShoppingCart> findById(String identifier);
}
