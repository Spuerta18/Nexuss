package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.BusinessRuleViolationException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.ReturnRequestRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeOrderAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Lets a buyer request the return of one of its delivered orders. An order
 * has at most one return request.
 */
@Service
public class RequestReturnService {

    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final AuthorizeOrderAccessService authorizeOrderAccessService;

    public RequestReturnService(ReturnRequestRepositoryPort returnRequestRepositoryPort,
                                OrderRepositoryPort orderRepositoryPort,
                                ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                AuthorizeOrderAccessService authorizeOrderAccessService,
                                RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.returnRequestRepositoryPort = returnRequestRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.authorizeOrderAccessService = authorizeOrderAccessService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public ReturnRequest execute(User actor, ReturnRequest returnRequest) {
        String returnRequestId = returnRequest.getIdentifier();
        String orderId = returnRequest.getOrder().getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.BUYER);
        Order order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("No order found with id " + orderId));
        // With the role restricted to BUYER above, this only lets the owning buyer through.
        authorizeOrderAccessService.execute(actor, order);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessRuleViolationException("Order " + order.getIdentifier() + " is "
                    + order.getStatus().getCode() + "; only DELIVERED orders can be returned");
        }
        if (returnRequestRepositoryPort.findById(returnRequestId).isPresent()) {
            throw new DuplicateResourceException(
                    "A return request with id " + returnRequestId + " already exists");
        }
        if (returnRequestRepositoryPort.findByOrderId(order.getIdentifier()).isPresent()) {
            throw new DuplicateResourceException("Order " + order.getIdentifier() + " already has a return request");
        }
        ReturnRequest saved = returnRequestRepositoryPort.save(
                new ReturnRequest(returnRequestId, order, order.getBuyer(), returnRequest.getReason()));
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.RETURN_REQUEST, actor, AffectedEntityType.RETURN_REQUEST, returnRequestId),
                AuditDetails.of("orderId", order.getIdentifier(), "newStatus", saved.getStatus().getCode()));
        return saved;
    }
}
