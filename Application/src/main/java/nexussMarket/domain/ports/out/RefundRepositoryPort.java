package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.exceptions.ConcurrentUpdateException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Refund;

public interface RefundRepositoryPort {

    /**
     * Stores a new refund. Fails with {@link DuplicateResourceException} if a refund with the same
     * identifier, or one for the same return request, already exists; the stored one is never replaced.
     */
    Refund insert(Refund refund);

    /**
     * Stores the changes of an existing refund. Fails with {@link ConcurrentUpdateException} if it was saved by
     * someone else since it was read (its version changed); nothing is stored then. The new version is set on the
     * argument.
     */
    Refund save(Refund refund);

    Optional<Refund> findById(String identifier);

    Optional<Refund> findByReturnRequestId(String returnRequestId);
}
