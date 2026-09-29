package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.BusinessRuleViolationException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.RequestReturnUseCase;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.ReturnRequestRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeOrderAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Lets a buyer request the return of one of its delivered orders. An order
 * has at most one return request.
 */
public class RequestReturnService implements RequestReturnUseCase {

    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final AuthorizeOrderAccessService authorizeOrderAccessService;

    public RequestReturnService(ReturnRequestRepositoryPort returnRequestRepositoryPort,
                                OrderRepositoryPort orderRepositoryPort,
                                ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                AuthorizeOrderAccessService authorizeOrderAccessService) {
        this.returnRequestRepositoryPort = returnRequestRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.authorizeOrderAccessService = authorizeOrderAccessService;
    }

    @Override
    public ReturnRequest execute(User actor, Command command) {
        validateRoleAuthorizationService.execute(actor, SystemRole.BUYER);
        Order order = orderRepositoryPort.findById(command.orderId())
                .orElseThrow(() -> new EntityNotFoundException("No order found with id " + command.orderId()));
        // With the role restricted to BUYER above, this only lets the owning buyer through.
        authorizeOrderAccessService.execute(actor, order);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessRuleViolationException("Order " + order.getIdentifier() + " is "
                    + order.getStatus().getCode() + "; only DELIVERED orders can be returned");
        }
        if (returnRequestRepositoryPort.findById(command.returnRequestId()).isPresent()) {
            throw new DuplicateResourceException(
                    "A return request with id " + command.returnRequestId() + " already exists");
        }
        if (returnRequestRepositoryPort.findByOrderId(order.getIdentifier()).isPresent()) {
            throw new DuplicateResourceException("Order " + order.getIdentifier() + " already has a return request");
        }
        return returnRequestRepositoryPort.save(
                new ReturnRequest(command.returnRequestId(), order, order.getBuyer(), command.reason()));
    }
}
