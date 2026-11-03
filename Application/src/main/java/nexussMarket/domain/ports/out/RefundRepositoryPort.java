package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Refund;

public interface RefundRepositoryPort {

    /**
     * Stores a new refund. Fails with {@link DuplicateResourceException} if a refund with the same
     * identifier, or one for the same return request, already exists; the stored one is never replaced.
     */
    Refund insert(Refund refund);

    /** Stores the changes of an existing refund. */
    Refund save(Refund refund);

    Optional<Refund> findById(String identifier);

    Optional<Refund> findByReturnRequestId(String returnRequestId);
}
