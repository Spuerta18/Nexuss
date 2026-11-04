package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.RefundRepositoryPort;
import nexussMarket.domain.ports.out.ReturnRequestRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.ReturnStatus;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Approves a return request: every order line goes back to the warehouse it
 * was shipped from, and a {@code PENDING} {@link Refund} for the order total
 * is created. Only administrators may approve: an order can hold lines of
 * several sellers, so no single seller has authority over the whole return.
 *
 * <p>Order of effects: versioned return request save, refund insert, stock
 * returns (retried on conflicts), audit.</p>
 */
@Service
public class ApproveReturnService {

    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private final RefundRepositoryPort refundRepositoryPort;
    private final RegisterInventoryReturnService registerInventoryReturnService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ApproveReturnService(ReturnRequestRepositoryPort returnRequestRepositoryPort,
                                RefundRepositoryPort refundRepositoryPort,
                                RegisterInventoryReturnService registerInventoryReturnService,
                                ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.returnRequestRepositoryPort = returnRequestRepositoryPort;
        this.refundRepositoryPort = refundRepositoryPort;
        this.registerInventoryReturnService = registerInventoryReturnService;
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
        // Validated before anything is stored: a request already decided fails here.
        returnRequest.approve(actor);
        // Saved with its version before any side effect: of two concurrent approvals only one gets past this line,
        // so stock is returned and a refund created exactly once.
        ReturnRequest saved = returnRequestRepositoryPort.save(returnRequest);
        // One refund per return request, so its identifier derives from the request's.
        Refund refund = refundRepositoryPort.insert(new Refund("REF-" + returnRequest.getIdentifier(), returnRequest,
                OrderTotals.totalOf(returnRequest.getOrder())));
        for (OrderLine line : returnRequest.getOrder().getLines()) {
            if (!line.holdsStock()) {
                continue;
            }
            registerInventoryReturnService.execute(actor, line.getProduct(), line.getWarehouse(), line.getQuantity());
        }
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.RETURN_APPROVAL, actor, AffectedEntityType.RETURN_REQUEST, returnRequestId),
                AuditDetails.of("previousStatus", previousStatus.getCode(),
                        "newStatus", returnRequest.getStatus().getCode(),
                        "orderId", returnRequest.getOrder().getIdentifier(), "refundId", refund.getIdentifier()));
        return saved;
    }
}
