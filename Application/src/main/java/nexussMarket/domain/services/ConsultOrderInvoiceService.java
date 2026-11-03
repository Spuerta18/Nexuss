package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Invoice;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.InvoiceRepositoryPort;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeOrderAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * The invoice of an order, issued when the order is paid. Readable by the
 * buyer who placed the order, administrators and supervisors
 * ({@link AuthorizeOrderAccessService} decides ownership). Fails with
 * {@link EntityNotFoundException} if the order does not exist or has no
 * invoice yet.
 */
@Service
public class ConsultOrderInvoiceService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final InvoiceRepositoryPort invoiceRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final AuthorizeOrderAccessService authorizeOrderAccessService;

    public ConsultOrderInvoiceService(OrderRepositoryPort orderRepositoryPort,
                                      InvoiceRepositoryPort invoiceRepositoryPort,
                                      ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                      AuthorizeOrderAccessService authorizeOrderAccessService) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.invoiceRepositoryPort = invoiceRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.authorizeOrderAccessService = authorizeOrderAccessService;
    }

    public Invoice execute(User actor, Order orderReference) {
        String orderId = orderReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.BUYER, SystemRole.ADMINISTRATOR,
                SystemRole.SUPERVISOR);
        Order order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("No order found with id " + orderId));
        authorizeOrderAccessService.execute(actor, order);
        return invoiceRepositoryPort.findByOrderId(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order " + orderId + " has no invoice yet"));
    }
}
