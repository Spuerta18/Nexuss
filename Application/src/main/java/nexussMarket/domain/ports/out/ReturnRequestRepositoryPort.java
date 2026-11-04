package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.exceptions.ConcurrentUpdateException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.ReturnRequest;

public interface ReturnRequestRepositoryPort {

    /**
     * Stores a new return request. Fails with {@link DuplicateResourceException} if a return request with the same
     * identifier, or one for the same order, already exists; the stored one is never replaced.
     */
    ReturnRequest insert(ReturnRequest returnRequest);

    /**
     * Stores the changes of an existing return request. Fails with {@link ConcurrentUpdateException} if it was saved by
     * someone else since it was read (its version changed); nothing is stored then. The new version is set on the
     * argument.
     */
    ReturnRequest save(ReturnRequest returnRequest);

    Optional<ReturnRequest> findById(String identifier);

    Optional<ReturnRequest> findByOrderId(String orderId);
}
