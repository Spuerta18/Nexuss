package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.RefundRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.RefundStatus;
import nexussMarket.domain.valueobjects.SystemRole;

/** Pays a pending refund back to the buyer. Only administrators may process refunds. */
@Service
public class ProcessRefundService {

    private final RefundRepositoryPort refundRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ProcessRefundService(RefundRepositoryPort refundRepositoryPort,
                                ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.refundRepositoryPort = refundRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public Refund execute(User actor, Refund refundReference) {
        String refundId = refundReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR);
        Refund refund = refundRepositoryPort.findById(refundId)
                .orElseThrow(() -> new EntityNotFoundException("No refund found with id " + refundId));
        RefundStatus previousStatus = refund.getStatus();
        refund.process();
        Refund saved = refundRepositoryPort.save(refund);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.REFUND_PROCESSING, actor, AffectedEntityType.REFUND, refundId),
                AuditDetails.of("previousStatus", previousStatus.getCode(),
                        "newStatus", refund.getStatus().getCode(),
                        "amount", refund.getAmount().getAmount().toPlainString(),
                        "currency", refund.getAmount().getCurrency().getCode()));
        return saved;
    }
}
