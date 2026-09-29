package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.ApproveReturnUseCase;
import nexussMarket.domain.ports.in.RegisterInventoryReturnUseCase;
import nexussMarket.domain.ports.out.RefundRepositoryPort;
import nexussMarket.domain.ports.out.ReturnRequestRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Approves a return request: every order line goes back to the warehouse it
 * was shipped from, and a {@code PENDING} {@link Refund} for the order total
 * is created. Only administrators may approve: an order can hold lines of
 * several sellers, so no single seller has authority over the whole return.
 */
public class ApproveReturnService implements ApproveReturnUseCase {

    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private final RefundRepositoryPort refundRepositoryPort;
    private final RegisterInventoryReturnUseCase registerInventoryReturnUseCase;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ApproveReturnService(ReturnRequestRepositoryPort returnRequestRepositoryPort,
                                RefundRepositoryPort refundRepositoryPort,
                                RegisterInventoryReturnUseCase registerInventoryReturnUseCase,
                                ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.returnRequestRepositoryPort = returnRequestRepositoryPort;
        this.refundRepositoryPort = refundRepositoryPort;
        this.registerInventoryReturnUseCase = registerInventoryReturnUseCase;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    @Override
    public ReturnRequest execute(User actor, Command command) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR);
        ReturnRequest returnRequest = returnRequestRepositoryPort.findById(command.returnRequestId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No return request found with id " + command.returnRequestId()));
        // Validated before any stock is returned: a request already decided fails here.
        returnRequest.approve(actor);
        for (OrderLine line : returnRequest.getOrder().getLines()) {
            registerInventoryReturnUseCase.execute(actor, new RegisterInventoryReturnUseCase.Command(
                    line.getProduct().getIdentifier(), line.getWarehouse().getIdentifier(), line.getQuantity()));
        }
        ReturnRequest saved = returnRequestRepositoryPort.save(returnRequest);
        // One refund per return request, so its identifier derives from the request's.
        refundRepositoryPort.save(new Refund("REF-" + returnRequest.getIdentifier(), returnRequest,
                OrderTotals.totalOf(returnRequest.getOrder())));
        return saved;
    }
}
