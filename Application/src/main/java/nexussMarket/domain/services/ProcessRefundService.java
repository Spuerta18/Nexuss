package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.RefundRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/** Pays a pending refund back to the buyer. Only administrators may process refunds. */
@Service
public class ProcessRefundService {

    private final RefundRepositoryPort refundRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ProcessRefundService(RefundRepositoryPort refundRepositoryPort,
                                ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.refundRepositoryPort = refundRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public Refund execute(User actor, Refund refundReference) {
        String refundId = refundReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR);
        Refund refund = refundRepositoryPort.findById(refundId)
                .orElseThrow(() -> new EntityNotFoundException("No refund found with id " + refundId));
        refund.process();
        return refundRepositoryPort.save(refund);
    }
}
