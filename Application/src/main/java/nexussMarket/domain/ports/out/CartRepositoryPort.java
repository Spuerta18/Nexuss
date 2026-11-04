package nexussMarket.domain.ports.out;

import java.util.List;
import java.util.Optional;

import nexussMarket.domain.exceptions.ConcurrentUpdateException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.ShoppingCart;

public interface CartRepositoryPort {

    /**
     * Stores a new cart. Fails with {@link DuplicateResourceException} if a cart with the same
     * identifier, or one of the same buyer, already exists; the stored one is never replaced.
     */
    ShoppingCart insert(ShoppingCart cart);

    /**
     * Stores the changes of an existing cart. Fails with {@link ConcurrentUpdateException} if it was saved by
     * someone else since it was read (its version changed); nothing is stored then. The new version is set on the
     * argument.
     */
    ShoppingCart save(ShoppingCart cart);

    Optional<ShoppingCart> findById(String identifier);

    List<ShoppingCart> findByBuyerId(String buyerId);

    /**
     * Deletes {@code cart} only if it is still at the version it was read with; otherwise fails with
     * {@link ConcurrentUpdateException} and deletes nothing.
     */
    void delete(ShoppingCart cart);
}
