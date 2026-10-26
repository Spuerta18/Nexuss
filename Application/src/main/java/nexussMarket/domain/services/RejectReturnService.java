package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.ReturnRequestRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.ReturnStatus;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Rejects a return request. Stock and refunds are not affected. Only
 * administrators may reject: an order can hold lines of several sellers, so no
 * single seller has authority over the whole return.
 */
@Service
public class RejectReturnService {

    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public RejectReturnService(ReturnRequestRepositoryPort returnRequestRepositoryPort,
                               ValidateRoleAuthorizationService validateRoleAuthorizationService,
                               RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.returnRequestRepositoryPort = returnRequestRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public ReturnRequest execute(User actor, ReturnRequest returnRequestReference) {
        String returnRequestId = returnRequestReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR);
        ReturnRequest returnRequest = returnRequestRepositoryPort.findById(returnRequestId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No return request found with id " + returnRequestId));
        ReturnStatus previousStatus = returnRequest.getStatus();
        returnRequest.reject(actor);
        ReturnRequest saved = returnRequestRepositoryPort.save(returnRequest);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.RETURN_REJECTION, actor, AffectedEntityType.RETURN_REQUEST, returnRequestId),
                AuditDetails.of("previousStatus", previousStatus.getCode(),
                        "newStatus", returnRequest.getStatus().getCode()));
        return saved;
    }
}
