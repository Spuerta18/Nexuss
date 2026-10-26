package nexussMarket.domain.services;

import java.util.List;

import org.springframework.stereotype.Service;

import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.OperationRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/** Returns the operation history, most recent first. Only administrators and supervisors may consult it. */
@Service
public class ConsultOperationsService {

    private final OperationRepositoryPort operationRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ConsultOperationsService(OperationRepositoryPort operationRepositoryPort,
                                    ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.operationRepositoryPort = operationRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public List<Operation> execute(User actor) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        return operationRepositoryPort.findAll();
    }
}
