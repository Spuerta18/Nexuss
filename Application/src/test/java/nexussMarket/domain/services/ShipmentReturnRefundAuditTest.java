package nexussMarket.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;

import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.RefundRepositoryPort;
import nexussMarket.domain.ports.out.ReturnRequestRepositoryPort;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.ShipmentStatus;

/** Shipment, return and refund changes record their operation and audit log. */
class ShipmentReturnRefundAuditTest extends AuditedServiceTestSupport {

    private final ShipmentRepositoryPort shipmentRepositoryPort = mock(ShipmentRepositoryPort.class);
    private final OrderRepositoryPort orderRepositoryPort = mock(OrderRepositoryPort.class);
    private final ReturnRequestRepositoryPort returnRequestRepositoryPort = mock(ReturnRequestRepositoryPort.class);
    private final RefundRepositoryPort refundRepositoryPort = mock(RefundRepositoryPort.class);
    private final ConfirmInventoryOutboundService outboundService = mock(ConfirmInventoryOutboundService.class);
    private final RegisterInventoryReturnService inventoryReturnService = mock(RegisterInventoryReturnService.class);

    private final Buyer buyer = buyer();
    private final LogisticsOperator operator = logisticsOperator();
    private final Administrator admin = administrator();

    @BeforeEach
    void repositoriesReturnWhatTheyStore() {
        when(shipmentRepositoryPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(orderRepositoryPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(returnRequestRepositoryPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(refundRepositoryPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(shipmentRepositoryPort.findById("SH-1")).thenReturn(Optional.empty());
        when(shipmentRepositoryPort.findByOrderId("O-1")).thenReturn(Optional.empty());
        when(returnRequestRepositoryPort.findById("R-1")).thenReturn(Optional.empty());
        when(returnRequestRepositoryPort.findByOrderId("O-1")).thenReturn(Optional.empty());
    }

    @Test
    void shipmentCreationIsRecorded() {
        when(orderRepositoryPort.findById("O-1")).thenReturn(Optional.of(order(buyer, OrderStatus.PAID)));

        new CreateShipmentService(shipmentRepositoryPort, orderRepositoryPort, validateRole, audit)
                .execute(operator, new Shipment("SH-1", new Order("O-1", null, null)));

        AuditLog entry = assertAudited(OperationType.SHIPMENT_CREATION, operator, AffectedEntityType.SHIPMENT, "SH-1");
        assertEquals("O-1", entry.getDetails().get("orderId"));
        assertEquals("CREATED", entry.getDetails().get("newStatus"));
    }

    @Test
    void shipmentPackingRecordsPreviousAndNewStatus() {
        shipment(OrderStatus.PAID, ShipmentStatus.CREATED);

        new PackShipmentService(shipmentRepositoryPort, validateRole, audit).execute(operator, reference());

        AuditLog entry = assertAudited(OperationType.SHIPMENT_PACKING, operator, AffectedEntityType.SHIPMENT, "SH-1");
        assertEquals("CREATED", entry.getDetails().get("previousStatus"));
        assertEquals("PACKED", entry.getDetails().get("newStatus"));
    }

    @Test
    void shipmentDispatchRecordsShipmentAndOrderStatus() {
        shipment(OrderStatus.PAID, ShipmentStatus.PACKED);

        new DispatchShipmentService(shipmentRepositoryPort, orderRepositoryPort, outboundService, validateRole, audit)
                .execute(operator, reference());

        AuditLog entry = assertAudited(OperationType.SHIPMENT_DISPATCH, operator, AffectedEntityType.SHIPMENT, "SH-1");
        assertEquals("PACKED", entry.getDetails().get("previousStatus"));
        assertEquals("DISPATCHED", entry.getDetails().get("newStatus"));
        assertEquals("SHIPPED", entry.getDetails().get("newOrderStatus"));
    }

    @Test
    void deliveryConfirmationRecordsShipmentAndOrderStatus() {
        shipment(OrderStatus.SHIPPED, ShipmentStatus.DISPATCHED);

        new ConfirmDeliveryService(shipmentRepositoryPort, orderRepositoryPort, validateRole, audit)
                .execute(operator, reference());

        AuditLog entry = assertAudited(OperationType.SHIPMENT_DELIVERY, operator, AffectedEntityType.SHIPMENT, "SH-1");
        assertEquals("DISPATCHED", entry.getDetails().get("previousStatus"));
        assertEquals("DELIVERED", entry.getDetails().get("newStatus"));
        assertEquals("DELIVERED", entry.getDetails().get("newOrderStatus"));
    }

    @Test
    void returnRequestIsRecorded() {
        when(orderRepositoryPort.findById("O-1")).thenReturn(Optional.of(order(buyer, OrderStatus.DELIVERED)));

        new RequestReturnService(returnRequestRepositoryPort, orderRepositoryPort, validateRole, authorizeOrder, audit)
                .execute(buyer, new ReturnRequest("R-1", new Order("O-1", null, null), null, "Broken"));

        AuditLog entry = assertAudited(OperationType.RETURN_REQUEST, buyer, AffectedEntityType.RETURN_REQUEST, "R-1");
        assertEquals("REQUESTED", entry.getDetails().get("newStatus"));
    }

    @Test
    void returnApprovalRecordsStatusesAndRefund() {
        requestedReturn();

        new ApproveReturnService(returnRequestRepositoryPort, refundRepositoryPort, inventoryReturnService,
                validateRole, audit).execute(admin, new ReturnRequest("R-1", null, null, null));

        AuditLog entry = assertAudited(OperationType.RETURN_APPROVAL, admin, AffectedEntityType.RETURN_REQUEST, "R-1");
        assertEquals("REQUESTED", entry.getDetails().get("previousStatus"));
        assertEquals("APPROVED", entry.getDetails().get("newStatus"));
        assertEquals("REF-R-1", entry.getDetails().get("refundId"));
    }

    @Test
    void returnRejectionRecordsPreviousAndNewStatus() {
        requestedReturn();

        new RejectReturnService(returnRequestRepositoryPort, validateRole, audit)
                .execute(admin, new ReturnRequest("R-1", null, null, null));

        AuditLog entry = assertAudited(OperationType.RETURN_REJECTION, admin, AffectedEntityType.RETURN_REQUEST, "R-1");
        assertEquals("REJECTED", entry.getDetails().get("newStatus"));
    }

    @Test
    void refundProcessingRecordsStatusesAndAmount() {
        Order order = order(buyer, OrderStatus.DELIVERED);
        Refund refund = new Refund("REF-1", new ReturnRequest("R-1", order, buyer, "Broken"), cop(2000));
        when(refundRepositoryPort.findById("REF-1")).thenReturn(Optional.of(refund));

        new ProcessRefundService(refundRepositoryPort, validateRole, audit)
                .execute(admin, new Refund("REF-1", null, null));

        AuditLog entry = assertAudited(OperationType.REFUND_PROCESSING, admin, AffectedEntityType.REFUND, "REF-1");
        assertEquals("PENDING", entry.getDetails().get("previousStatus"));
        assertEquals("PROCESSED", entry.getDetails().get("newStatus"));
        assertEquals("2000", entry.getDetails().get("amount"));
        assertEquals("COP", entry.getDetails().get("currency"));
    }

    private void shipment(OrderStatus orderStatus, ShipmentStatus shipmentStatus) {
        Shipment shipment = new Shipment("SH-1", order(buyer, orderStatus), shipmentStatus, LocalDateTime.now(),
                null, null, null);
        when(shipmentRepositoryPort.findById("SH-1")).thenReturn(Optional.of(shipment));
    }

    private static Shipment reference() {
        return new Shipment("SH-1", null);
    }

    private void requestedReturn() {
        ReturnRequest returnRequest = new ReturnRequest("R-1", order(buyer, OrderStatus.DELIVERED), buyer, "Broken");
        when(returnRequestRepositoryPort.findById("R-1")).thenReturn(Optional.of(returnRequest));
    }
}
