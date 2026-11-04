package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.InvalidOrderStatusTransitionException;
import nexussMarket.domain.exceptions.PaymentDeclinedException;
import nexussMarket.domain.models.Invoice;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.PaymentResult;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.InvoiceRepositoryPort;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.PaymentGatewayPort;
import nexussMarket.domain.services.authorization.AuthorizeOrderAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * The buyer pays one of their own {@code PENDING_PAYMENT} orders through the
 * payment gateway ({@link PaymentGatewayPort}).
 *
 * <ul>
 *   <li>Approved: the order moves to {@code PAID} (versioned save: of two
 *       concurrent payments only one passes, 409 for the other), its invoice is
 *       inserted, the payment is audited ({@code ORDER_PAYMENT}), and an order
 *       with only digital products is delivered at once
 *       ({@link FulfillDigitalOrderService}).</li>
 *   <li>Declined: the order stays {@code PENDING_PAYMENT}, the attempt is
 *       audited ({@code PAYMENT_DECLINED}) and {@link PaymentDeclinedException}
 *       answers {@code 402}.</li>
 *   <li>An order that is not {@code PENDING_PAYMENT} (already paid, cancelled)
 *       fails before the gateway is called (409).</li>
 * </ul>
 *
 * <p>The payment token is never stored nor audited.</p>
 */
@Service
public class PayOrderService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final InvoiceRepositoryPort invoiceRepositoryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final AuthorizeOrderAccessService authorizeOrderAccessService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final FulfillDigitalOrderService fulfillDigitalOrderService;

    public PayOrderService(OrderRepositoryPort orderRepositoryPort, InvoiceRepositoryPort invoiceRepositoryPort,
                           PaymentGatewayPort paymentGatewayPort,
                           ValidateRoleAuthorizationService validateRoleAuthorizationService,
                           AuthorizeOrderAccessService authorizeOrderAccessService,
                           RegisterOperationAndAuditService registerOperationAndAuditService,
                           FulfillDigitalOrderService fulfillDigitalOrderService) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.invoiceRepositoryPort = invoiceRepositoryPort;
        this.paymentGatewayPort = paymentGatewayPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.authorizeOrderAccessService = authorizeOrderAccessService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
        this.fulfillDigitalOrderService = fulfillDigitalOrderService;
    }

    public Order execute(User actor, Order orderReference, String paymentToken) {
        String orderId = orderReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.BUYER);
        Order order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("No order found with id " + orderId));
        // With the role restricted to BUYER above, this only lets the owning buyer through.
        authorizeOrderAccessService.execute(actor, order);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStatusTransitionException("Order " + orderId + " is " + order.getStatus().getCode()
                    + "; only PENDING_PAYMENT orders can be paid");
        }
        Money total = OrderTotals.totalOf(order);
        PaymentResult result = paymentGatewayPort.charge(orderId, total, paymentToken);
        if (!result.approved()) {
            registerOperationAndAuditService.execute(
                    Operation.of(OperationType.PAYMENT_DECLINED, actor, AffectedEntityType.ORDER, orderId),
                    AuditDetails.of("status", order.getStatus().getCode(),
                            "amount", total.getAmount().toPlainString(), "currency", total.getCurrency().getCode(),
                            "reason", result.reason()));
            throw new PaymentDeclinedException("The payment of order " + orderId + " was declined: " + result.reason());
        }
        OrderStatus previousStatus = order.getStatus();
        order.advanceTo(OrderStatus.PAID);
        // Saved with its version before the invoice exists, so a concurrent second payment fails here (409).
        Order saved = orderRepositoryPort.save(order);
        Invoice invoice = invoiceRepositoryPort.insert(AdvanceOrderStatusService.invoiceFor(saved));
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.ORDER_PAYMENT, actor, AffectedEntityType.ORDER, orderId),
                AuditDetails.of("previousStatus", previousStatus.getCode(), "newStatus", saved.getStatus().getCode(),
                        "invoiceId", invoice.getIdentifier(), "paymentReference", result.reference(),
                        "amount", total.getAmount().toPlainString(), "currency", total.getCurrency().getCode()));
        return fulfillDigitalOrderService.execute(actor, saved);
    }
}
