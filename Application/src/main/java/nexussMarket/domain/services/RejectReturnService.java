package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.RejectReturnUseCase;
import nexussMarket.domain.ports.out.ReturnRequestRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Rejects a return request. Stock and refunds are not affected. Only
 * administrators may reject: an order can hold lines of several sellers, so no
 * single seller has authority over the whole return.
 */
public class RejectReturnService implements RejectReturnUseCase {

    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public RejectReturnService(ReturnRequestRepositoryPort returnRequestRepositoryPort,
                               ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.returnRequestRepositoryPort = returnRequestRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    @Override
    public ReturnRequest execute(User actor, Command command) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR);
        ReturnRequest returnRequest = returnRequestRepositoryPort.findById(command.returnRequestId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No return request found with id " + command.returnRequestId()));
        returnRequest.reject(actor);
        return returnRequestRepositoryPort.save(returnRequest);
    }
}
