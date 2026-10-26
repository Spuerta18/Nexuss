package nexussMarket.domain.services;

import nexussMarket.domain.models.Operation;
import nexussMarket.domain.ports.out.OperationRepositoryPort;

/**
 * Internal service, not exposed as a use case. Appends an {@link Operation} to
 * the operation history. Services do not call it directly: they use
 * {@link RegisterOperationAndAuditService}, which also writes the audit log.
 */
public class RegisterOperationService {

    private final OperationRepositoryPort operationRepositoryPort;

    public RegisterOperationService(OperationRepositoryPort operationRepositoryPort) {
        this.operationRepositoryPort = operationRepositoryPort;
    }

    public Operation execute(Operation operation) {
        return operationRepositoryPort.save(operation);
    }
}
