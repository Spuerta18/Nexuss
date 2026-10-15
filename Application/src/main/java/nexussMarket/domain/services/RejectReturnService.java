package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.ReturnRequestRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Rejects a return request. Stock and refunds are not affected. Only
 * administrators may reject: an order can hold lines of several sellers, so no
 * single seller has authority over the whole return.
 */
@Service
public class RejectReturnService {

    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public RejectReturnService(ReturnRequestRepositoryPort returnRequestRepositoryPort,
                               ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.returnRequestRepositoryPort = returnRequestRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public ReturnRequest execute(User actor, ReturnRequest returnRequestReference) {
        String returnRequestId = returnRequestReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR);
        ReturnRequest returnRequest = returnRequestRepositoryPort.findById(returnRequestId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No return request found with id " + returnRequestId));
        returnRequest.reject(actor);
        return returnRequestRepositoryPort.save(returnRequest);
    }
}
