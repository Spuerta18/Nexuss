package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.User;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Records the payment of an order: an administrator moves it from
 * {@code PENDING_PAYMENT} to {@code PAID} through
 * {@link AdvanceOrderStatusService}, which validates the transition, issues
 * the invoice and audits {@code ORDER_STATUS_ADVANCE}.
 */
@Service
public class RegisterOrderPaymentService {

    private final AdvanceOrderStatusService advanceOrderStatusService;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public RegisterOrderPaymentService(AdvanceOrderStatusService advanceOrderStatusService,
                                       ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.advanceOrderStatusService = advanceOrderStatusService;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public Order execute(User actor, Order orderReference) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR);
        return advanceOrderStatusService.execute(actor, orderReference, OrderStatus.PAID);
    }
}
