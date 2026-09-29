package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.ProcessRefundUseCase;
import nexussMarket.domain.ports.out.RefundRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/** Pays a pending refund back to the buyer. Only administrators may process refunds. */
public class ProcessRefundService implements ProcessRefundUseCase {

    private final RefundRepositoryPort refundRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ProcessRefundService(RefundRepositoryPort refundRepositoryPort,
                                ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.refundRepositoryPort = refundRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    @Override
    public Refund execute(User actor, Command command) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR);
        Refund refund = refundRepositoryPort.findById(command.refundId())
                .orElseThrow(() -> new EntityNotFoundException("No refund found with id " + command.refundId()));
        refund.process();
        return refundRepositoryPort.save(refund);
    }
}
