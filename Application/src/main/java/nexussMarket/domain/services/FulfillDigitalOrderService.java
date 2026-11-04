package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.ProductType;

/**
 * Internal service, run right after an order is paid. An order holding only
 * digital products needs no shipment: it goes through the valid transitions
 * {@code PAID -> SHIPPED -> DELIVERED} at once, is saved with its version and
 * audited ({@code DIGITAL_ORDER_FULFILLMENT}). Any other order is returned
 * unchanged and follows the shipment lifecycle.
 */
@Service
public class FulfillDigitalOrderService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public FulfillDigitalOrderService(OrderRepositoryPort orderRepositoryPort,
                                      RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    /** {@code paidOrder} delivered when it holds only digital products; otherwise {@code paidOrder} itself. */
    public Order execute(User actor, Order paidOrder) {
        if (paidOrder.getStatus() != OrderStatus.PAID || !onlyDigital(paidOrder)) {
            return paidOrder;
        }
        paidOrder.advanceTo(OrderStatus.SHIPPED);
        paidOrder.advanceTo(OrderStatus.DELIVERED);
        Order saved = orderRepositoryPort.save(paidOrder);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.DIGITAL_ORDER_FULFILLMENT, actor, AffectedEntityType.ORDER,
                        paidOrder.getIdentifier()),
                AuditDetails.of("previousStatus", OrderStatus.PAID.getCode(),
                        "newStatus", saved.getStatus().getCode()));
        return saved;
    }

    private static boolean onlyDigital(Order order) {
        return !order.getLines().isEmpty() && order.getLines().stream()
                .map(OrderLine::getProduct)
                .allMatch(product -> product.getProductType() == ProductType.DIGITAL);
    }
}
