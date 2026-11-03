package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.ReturnRequest;

public interface ReturnRequestRepositoryPort {

    /**
     * Stores a new return request. Fails with {@link DuplicateResourceException} if a return request with the same
     * identifier, or one for the same order, already exists; the stored one is never replaced.
     */
    ReturnRequest insert(ReturnRequest returnRequest);

    /** Stores the changes of an existing return request. */
    ReturnRequest save(ReturnRequest returnRequest);

    Optional<ReturnRequest> findById(String identifier);

    Optional<ReturnRequest> findByOrderId(String orderId);
}
