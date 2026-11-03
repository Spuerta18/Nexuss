package nexussMarket.domain.ports.out;

import java.util.List;
import java.util.Optional;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.ShoppingCart;

public interface CartRepositoryPort {

    /**
     * Stores a new cart. Fails with {@link DuplicateResourceException} if a cart with the same
     * identifier, or one of the same buyer, already exists; the stored one is never replaced.
     */
    ShoppingCart insert(ShoppingCart cart);

    /** Stores the changes of an existing cart. */
    ShoppingCart save(ShoppingCart cart);

    Optional<ShoppingCart> findById(String identifier);

    List<ShoppingCart> findByBuyerId(String buyerId);

    void deleteById(String identifier);
}
