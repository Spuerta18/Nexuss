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
    private final FulfillDigitalOrderService fulfillDigitalOrderService;

    public RegisterOrderPaymentService(AdvanceOrderStatusService advanceOrderStatusService,
                                       ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                       FulfillDigitalOrderService fulfillDigitalOrderService) {
        this.advanceOrderStatusService = advanceOrderStatusService;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.fulfillDigitalOrderService = fulfillDigitalOrderService;
    }

    public Order execute(User actor, Order orderReference) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR);
        Order paid = advanceOrderStatusService.execute(actor, orderReference, OrderStatus.PAID);
        // An order with only digital products is delivered right away (FulfillDigitalOrderService).
        return fulfillDigitalOrderService.execute(actor, paid);
    }
}
