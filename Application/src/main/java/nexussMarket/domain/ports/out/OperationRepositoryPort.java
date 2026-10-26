package nexussMarket.domain.ports.out;

import java.util.List;

import nexussMarket.domain.models.Operation;

/** Append-only store of {@link Operation}s: operations are added, never updated nor deleted. */
public interface OperationRepositoryPort {

    /** Appends {@code operation}; an existing identifier is never overwritten. */
    Operation save(Operation operation);

    /** Every operation, most recent first. */
    List<Operation> findAll();
}
