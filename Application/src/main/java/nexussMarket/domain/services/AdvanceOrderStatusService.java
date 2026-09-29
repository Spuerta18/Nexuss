package nexussMarket.domain.services;

import java.time.LocalDateTime;

import nexussMarket.domain.exceptions.BusinessRuleViolationException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Invoice;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.AdvanceOrderStatusUseCase;
import nexussMarket.domain.ports.out.InvoiceRepositoryPort;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Confirms the payment of an order, moving it from {@code PENDING_PAYMENT} to
 * {@code PAID}, and issues its {@link Invoice}. {@code SHIPPED} and
 * {@code DELIVERED} are reached only through the shipment lifecycle
 * ({@link DispatchShipmentService}, {@link ConfirmDeliveryService}). Only
 * logistics operators and administrators may advance an order; its buyer may
 * not.
 */
public class AdvanceOrderStatusService implements AdvanceOrderStatusUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final InvoiceRepositoryPort invoiceRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public AdvanceOrderStatusService(OrderRepositoryPort orderRepositoryPort,
                                     InvoiceRepositoryPort invoiceRepositoryPort,
                                     ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.invoiceRepositoryPort = invoiceRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    @Override
    public Order execute(User actor, Command command) {
        validateRoleAuthorizationService.execute(actor, SystemRole.LOGISTICS_OPERATOR, SystemRole.ADMINISTRATOR);
        if (command.targetStatus() == OrderStatus.SHIPPED || command.targetStatus() == OrderStatus.DELIVERED) {
            throw new BusinessRuleViolationException("Use the Shipment lifecycle to move an order to SHIPPED or"
                    + " DELIVERED, not AdvanceOrderStatusUseCase");
        }
        Order order = orderRepositoryPort.findById(command.orderId())
                .orElseThrow(() -> new EntityNotFoundException("No order found with id " + command.orderId()));
        order.advanceTo(command.targetStatus());
        Order saved = orderRepositoryPort.save(order);
        if (order.getStatus() == OrderStatus.PAID) {
            invoiceRepositoryPort.save(invoiceFor(order));
        }
        return saved;
    }

    /** The invoice of a just-paid order. One invoice per order, so its identifier derives from the order's. */
    private static Invoice invoiceFor(Order order) {
        return new Invoice("INV-" + order.getIdentifier(), order, order.getBuyer(), OrderTotals.totalOf(order),
                LocalDateTime.now());
    }
}
