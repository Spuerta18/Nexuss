package nexussMarket.domain.services;

/**
 * Internal service, not exposed as a use case. Injected as a constructor
 * dependency by services that need to audit an operation.
 *
 * <p>No {@code AuditRepositoryPort} exists yet, so this is a conscious
 * placeholder: it only logs, it does not persist.</p>
 */
public class AuditOperationService {

    public void record(String userId, String operation) {
        System.out.println("[AUDIT] userId=" + userId + " operation=" + operation);
    }
}
