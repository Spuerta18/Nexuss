package nexussMarket.adapters.in.rest.responses;

import java.time.LocalDateTime;

/** A recorded operation. The performer appears by identifier only. */
public record OperationResponse(
        String identifier,
        String operationType,
        LocalDateTime executionDate,
        String performedBy,
        String affectedEntityType,
        String affectedEntityId) {
}
