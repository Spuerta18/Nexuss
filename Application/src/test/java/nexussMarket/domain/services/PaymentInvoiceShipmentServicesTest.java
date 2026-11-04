package nexussMarket.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.InvalidOrderStatusTransitionException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Invoice;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.ports.out.InvoiceRepositoryPort;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.UserStatus;

/** Order payment by an administrator, and the invoice and shipment of an order for the allowed readers. */
class PaymentInvoiceShipmentServicesTest extends AuditedServiceTestSupport {

    private final OrderRepositoryPort orderRepositoryPort = mock(OrderRepositoryPort.class);
    private final InvoiceRepositoryPort invoiceRepositoryPort = mock(InvoiceRepositoryPort.class);
    private final ShipmentRepositoryPort shipmentRepositoryPort = mock(ShipmentRepositoryPort.class);

    private final Buyer owner = buyer();
    private final Buyer otherBuyer =
            new Buyer("B-2", "Bea Buyer", "bea@example.com", UserStatus.ACTIVE, CustomerStatus.ENABLED);

    private final RegisterOrderPaymentService payment = new RegisterOrderPaymentService(
            new AdvanceOrderStatusService(orderRepositoryPort, invoiceRepositoryPort, validateRole, audit),
            validateRole, new FulfillDigitalOrderService(orderRepositoryPort, audit));
    private final ConsultOrderInvoiceService invoice =
            new ConsultOrderInvoiceService(orderRepositoryPort, invoiceRepositoryPort, validateRole, authorizeOrder);
    private final ConsultOrderShipmentService shipment =
            new ConsultOrderShipmentService(orderRepositoryPort, shipmentRepositoryPort, validateRole, authorizeOrder);

    @BeforeEach
    void repositoriesReturnWhatTheyStore() {
        when(orderRepositoryPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(invoiceRepositoryPort.insert(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
    }

    @Test
    void anAdministratorRecordsThePaymentAndTheInvoiceIsIssued() {
        Administrator admin = administrator();
        storedOrder(OrderStatus.PENDING_PAYMENT);

        Order paid = payment.execute(admin, new Order("O-1", null, null));

        assertEquals(OrderStatus.PAID, paid.getStatus());
        ArgumentCaptor<Invoice> issued = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepositoryPort).insert(issued.capture());
        assertEquals("INV-O-1", issued.getValue().getIdentifier());
        assertEquals(cop(2000), issued.getValue().getTotalAmount());
        AuditLog entry = assertAudited(OperationType.ORDER_STATUS_ADVANCE, admin, AffectedEntityType.ORDER, "O-1");
        assertEquals("PAID", entry.getDetails().get("newStatus"));
    }

    @Test
    void onlyAnAdministratorRecordsAPayment() {
        storedOrder(OrderStatus.PENDING_PAYMENT);

        assertThrows(OperationNotAllowedException.class, () -> payment.execute(logisticsOperator(), orderReference()));
        assertThrows(OperationNotAllowedException.class, () -> payment.execute(owner, orderReference()));

        verify(orderRepositoryPort, never()).save(any());
        verifyNoInteractions(invoiceRepositoryPort);
        assertNothingAudited();
    }

    @Test
    void anOrderIsPaidOnlyOnce() {
        storedOrder(OrderStatus.PAID);

        assertThrows(InvalidOrderStatusTransitionException.class, () -> payment.execute(administrator(),
                orderReference()));
        verifyNoInteractions(invoiceRepositoryPort);
    }

    @Test
    void theInvoiceIsReadByItsBuyerAdministratorsAndSupervisors() {
        Order order = storedOrder(OrderStatus.PAID);
        Invoice issued = new Invoice("INV-O-1", order, owner, cop(2000), LocalDateTime.now());
        when(invoiceRepositoryPort.findByOrderId("O-1")).thenReturn(Optional.of(issued));

        assertSame(issued, invoice.execute(owner, orderReference()));
        assertSame(issued, invoice.execute(administrator(), orderReference()));
        assertSame(issued, invoice.execute(supervisor(), orderReference()));
    }

    @Test
    void anotherBuyerSellerOrOperatorCannotReadTheInvoice() {
        storedOrder(OrderStatus.PAID);

        assertThrows(OperationNotAllowedException.class, () -> invoice.execute(otherBuyer, orderReference()));
        assertThrows(OperationNotAllowedException.class, () -> invoice.execute(seller(), orderReference()));
        assertThrows(OperationNotAllowedException.class, () -> invoice.execute(logisticsOperator(), orderReference()));
        verifyNoInteractions(invoiceRepositoryPort);
    }

    @Test
    void anUnpaidOrderHasNoInvoice() {
        storedOrder(OrderStatus.PENDING_PAYMENT);

        EntityNotFoundException error = assertThrows(EntityNotFoundException.class,
                () -> invoice.execute(owner, orderReference()));
        assertEquals("Order O-1 has no invoice yet", error.getMessage());
    }

    @Test
    void theShipmentIsReadOnlyByTheBuyerWhoPlacedTheOrder() {
        Order order = storedOrder(OrderStatus.SHIPPED);
        Shipment created = new Shipment("SH-1", order);
        when(shipmentRepositoryPort.findByOrderId("O-1")).thenReturn(Optional.of(created));

        assertSame(created, shipment.execute(owner, orderReference()));
        assertThrows(OperationNotAllowedException.class, () -> shipment.execute(otherBuyer, orderReference()));
        assertThrows(OperationNotAllowedException.class, () -> shipment.execute(administrator(), orderReference()));
    }

    @Test
    void anOrderWithoutShipmentAnswersNotFound() {
        storedOrder(OrderStatus.PAID);

        assertThrows(EntityNotFoundException.class, () -> shipment.execute(owner, orderReference()));
        assertThrows(EntityNotFoundException.class, () -> shipment.execute(owner, new Order("O-404", null, null)));
    }

    private Order storedOrder(OrderStatus status) {
        Order order = order(owner, status);
        when(orderRepositoryPort.findById("O-1")).thenReturn(Optional.of(order));
        return order;
    }

    private static Order orderReference() {
        return new Order("O-1", null, null);
    }
}
