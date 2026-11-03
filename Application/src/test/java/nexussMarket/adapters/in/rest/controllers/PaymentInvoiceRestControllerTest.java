package nexussMarket.adapters.in.rest.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.InvalidOrderStatusTransitionException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Invoice;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/** Payment by the administrator, the order invoice for buyer, administrator and supervisor, and the order shipment. */
class PaymentInvoiceRestControllerTest extends RestControllerTestSupport {

    @Test
    void administratorRecordsThePayment() throws Exception {
        when(administratorPort.registerOrderPayment(any(), any())).thenReturn(order(OrderStatus.PAID));

        mockMvc.perform(post("/api/admin/orders/O-1/payment").header(authorization(), bearer(administrator())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("O-1"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.total.amount").value(99800));

        ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        verify(administratorPort).registerOrderPayment(any(), order.capture());
        assertEquals("O-1", order.getValue().getIdentifier());
    }

    @Test
    void paymentIsForAdministratorsOnly() throws Exception {
        for (String token : List.of(bearer(buyer()), bearer(logisticsOperator()), bearer(supervisor()))) {
            mockMvc.perform(post("/api/admin/orders/O-1/payment").header(authorization(), token))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(administratorPort);
    }

    @Test
    void payingAnOrderTwiceAnswers409() throws Exception {
        when(administratorPort.registerOrderPayment(any(), any()))
                .thenThrow(new InvalidOrderStatusTransitionException("Cannot transition order O-1 from PAID to PAID"));

        mockMvc.perform(post("/api/admin/orders/O-1/payment").header(authorization(), bearer(administrator())))
                .andExpect(status().isConflict());
    }

    @Test
    void invoiceIsAvailableToTheBuyerAdministratorAndSupervisor() throws Exception {
        Invoice invoice = invoice();
        when(buyerPort.consultOrderInvoice(any(), any())).thenReturn(invoice);
        when(administratorPort.consultOrderInvoice(any(), any())).thenReturn(invoice);
        when(supervisorPort.consultOrderInvoice(any(), any())).thenReturn(invoice);

        mockMvc.perform(get("/api/buyer/orders/O-1/invoice").header(authorization(), bearer(buyer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("INV-O-1"))
                .andExpect(jsonPath("$.orderId").value("O-1"))
                .andExpect(jsonPath("$.buyerId").value("B-1"))
                .andExpect(jsonPath("$.totalAmount.amount").value(99800))
                .andExpect(jsonPath("$.totalAmount.currency").value("COP"));
        mockMvc.perform(get("/api/admin/orders/O-1/invoice").header(authorization(), bearer(administrator())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/supervisor/orders/O-1/invoice").header(authorization(), bearer(supervisor())))
                .andExpect(status().isOk());
    }

    @Test
    void anotherBuyersInvoiceAnswers403AndAMissingOneAnswers404() throws Exception {
        when(buyerPort.consultOrderInvoice(any(), any()))
                .thenThrow(new OperationNotAllowedException("User B-1 is not allowed to access order O-2"))
                .thenThrow(new EntityNotFoundException("Order O-3 has no invoice yet"));

        mockMvc.perform(get("/api/buyer/orders/O-2/invoice").header(authorization(), bearer(buyer())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/buyer/orders/O-3/invoice").header(authorization(), bearer(buyer())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order O-3 has no invoice yet"));
    }

    @Test
    void invoiceRoutesAreLimitedToTheirRole() throws Exception {
        mockMvc.perform(get("/api/buyer/orders/O-1/invoice").header(authorization(), bearer(seller())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/orders/O-1/invoice").header(authorization(), bearer(supervisor())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/supervisor/orders/O-1/invoice").header(authorization(), bearer(logisticsOperator())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/buyer/orders/O-1/invoice")).andExpect(status().isUnauthorized());
        verifyNoInteractions(buyerPort, administratorPort, supervisorPort);
    }

    @Test
    void buyerConsultsTheShipmentOfTheirOrder() throws Exception {
        when(buyerPort.consultOrderShipment(any(), any())).thenReturn(new Shipment("SH-1", order(OrderStatus.PAID)));

        mockMvc.perform(get("/api/buyer/orders/O-1/shipment").header(authorization(), bearer(buyer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("SH-1"))
                .andExpect(jsonPath("$.orderId").value("O-1"))
                .andExpect(jsonPath("$.status").value("CREATED"));
        mockMvc.perform(get("/api/buyer/orders/O-1/shipment").header(authorization(), bearer(logisticsOperator())))
                .andExpect(status().isForbidden());
    }

    @Test
    void anotherBuyersShipmentAnswers403() throws Exception {
        when(buyerPort.consultOrderShipment(any(), any()))
                .thenThrow(new OperationNotAllowedException("User B-1 is not allowed to access order O-2"));

        mockMvc.perform(get("/api/buyer/orders/O-2/shipment").header(authorization(), bearer(buyer())))
                .andExpect(status().isForbidden());
    }

    /** One line: 2 × 49 900 = 99 800 COP. */
    private static Order order(OrderStatus status) {
        Product product = new Product("P-1", "T-shirt", ProductType.PHYSICAL, ProductStatus.PUBLISHED, null,
                Money.of(new BigDecimal("49900"), Currency.COP), List.of());
        Warehouse warehouse = new Warehouse("WH-1", "North Warehouse", "1 Main Street", WarehouseOwnerType.MARKETPLACE);
        return new Order("O-1", buyer(), status,
                List.of(new OrderLine(product, product.getName(), 2, product.getPrice(), warehouse)),
                LocalDateTime.now());
    }

    private static Invoice invoice() {
        Order order = order(OrderStatus.PAID);
        Buyer buyer = order.getBuyer();
        return new Invoice("INV-O-1", order, buyer, order.getTotal().orElseThrow(), LocalDateTime.now());
    }
}
