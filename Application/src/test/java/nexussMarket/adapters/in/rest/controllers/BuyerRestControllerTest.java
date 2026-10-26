package nexussMarket.adapters.in.rest.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.http.MediaType;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.ReturnNotEligibleException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

class BuyerRestControllerTest extends RestControllerTestSupport {

    @Test
    void createCartAnswers201AndPassesTheAuthenticatedBuyer() throws Exception {
        Buyer buyer = buyer();
        when(buyerPort.createShoppingCart(any(), any())).thenReturn(new ShoppingCart("C-1", buyer));

        mockMvc.perform(post("/api/buyer/carts").header(authorization(), bearer(buyer))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cartId\":\"C-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identifier").value("C-1"))
                .andExpect(jsonPath("$.buyerId").value("B-1"));

        ArgumentCaptor<User> actor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<ShoppingCart> cart = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(buyerPort).createShoppingCart(actor.capture(), cart.capture());
        assertSame(buyer, actor.getValue());
        assertEquals("C-1", cart.getValue().getIdentifier());
    }

    @Test
    void addCartLineReturnsTheCartWithPrices() throws Exception {
        Buyer buyer = buyer();
        ShoppingCart cart = new ShoppingCart("C-1", buyer);
        cart.addLine(product(), 2);
        when(buyerPort.addCartLine(any(), any(), any(), eq(2))).thenReturn(cart);

        mockMvc.perform(post("/api/buyer/carts/C-1/lines").header(authorization(), bearer(buyer))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":\"P-1\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].productId").value("P-1"))
                .andExpect(jsonPath("$.lines[0].quantity").value(2))
                .andExpect(jsonPath("$.lines[0].unitPrice.amount").value(49900))
                .andExpect(jsonPath("$.lines[0].unitPrice.currency").value("COP"));
    }

    @Test
    void listOrdersUsesTheAuthenticatedBuyer() throws Exception {
        Buyer buyer = buyer();
        when(buyerPort.listBuyerOrders(any(), any())).thenReturn(List.of(order(buyer)));

        mockMvc.perform(get("/api/buyer/orders").header(authorization(), bearer(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].identifier").value("O-1"))
                .andExpect(jsonPath("$[0].status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$[0].lines[0].warehouseId").value("WH-1"));

        ArgumentCaptor<Buyer> owner = ArgumentCaptor.forClass(Buyer.class);
        verify(buyerPort).listBuyerOrders(any(), owner.capture());
        assertEquals("B-1", owner.getValue().getIdentifier());
    }

    @Test
    void requestReturnAnswers201() throws Exception {
        Buyer buyer = buyer();
        when(buyerPort.requestReturn(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        mockMvc.perform(post("/api/buyer/orders/O-1/returns").header(authorization(), bearer(buyer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"returnRequestId\":\"RR-1\",\"reason\":\"Wrong size\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identifier").value("RR-1"))
                .andExpect(jsonPath("$.orderId").value("O-1"))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    void withoutTokenAnswers401() throws Exception {
        mockMvc.perform(get("/api/buyer/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(buyerPort);
    }

    @Test
    void anotherRoleAnswers403() throws Exception {
        mockMvc.perform(get("/api/buyer/orders").header(authorization(), bearer(seller())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
        verifyNoInteractions(buyerPort);
    }

    @Test
    void invalidQuantityAnswers400() throws Exception {
        mockMvc.perform(post("/api/buyer/carts/C-1/lines").header(authorization(), bearer(buyer()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":\"\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
        verify(buyerPort, org.mockito.Mockito.never()).addCartLine(any(), any(), any(), anyInt());
    }

    @Test
    void domainExceptionsMapToCoherentStatuses() throws Exception {
        String token = bearer(buyer());
        when(buyerPort.consultOrder(any(), any())).thenThrow(new EntityNotFoundException("No order found with id X"));
        when(buyerPort.requestReturn(any(), any())).thenThrow(new ReturnNotEligibleException("Order X is PAID; only DELIVERED orders can be returned"));

        mockMvc.perform(get("/api/buyer/orders/X").header(authorization(), token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No order found with id X"));
        mockMvc.perform(post("/api/buyer/orders/X/returns").header(authorization(), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"returnRequestId\":\"R-1\",\"reason\":\"Broken\"}"))
                .andExpect(status().isConflict());
    }

    private static Product product() {
        return new Product("P-1", "T-shirt", ProductType.PHYSICAL, ProductStatus.PUBLISHED, null,
                Money.of(new BigDecimal("49900"), Currency.COP), List.of());
    }

    private static Order order(Buyer buyer) {
        Warehouse warehouse = new Warehouse("WH-1", "North Warehouse", "1 Main Street", WarehouseOwnerType.MARKETPLACE);
        OrderLine line = new OrderLine(product(), "T-shirt", 1, Money.of(new BigDecimal("49900"), Currency.COP),
                warehouse);
        return new Order("O-1", buyer, OrderStatus.PENDING_PAYMENT, List.of(line), LocalDateTime.now());
    }
}
