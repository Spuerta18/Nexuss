package nexussMarket.adapters.in.rest.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
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

/** The active-cart routes of the buyer, optional identifiers and the order total. */
class ActiveCartRestControllerTest extends RestControllerTestSupport {

    @Test
    void consultActiveCartReturnsTheBuyersCart() throws Exception {
        Buyer buyer = buyer();
        ShoppingCart cart = new ShoppingCart("CART-ab12cd34", buyer);
        cart.addLine(product("P-1", 49900), 2);
        when(buyerPort.consultActiveCart(any())).thenReturn(cart);

        mockMvc.perform(get("/api/buyer/cart").header(authorization(), bearer(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("CART-ab12cd34"))
                .andExpect(jsonPath("$.lines[0].quantity").value(2));

        ArgumentCaptor<User> actor = ArgumentCaptor.forClass(User.class);
        verify(buyerPort).consultActiveCart(actor.capture());
        assertSame(buyer, actor.getValue());
    }

    @Test
    void consultActiveCartAnswers404WithoutOne() throws Exception {
        when(buyerPort.consultActiveCart(any())).thenThrow(new EntityNotFoundException("Buyer B-1 has no active cart"));

        mockMvc.perform(get("/api/buyer/cart").header(authorization(), bearer(buyer())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Buyer B-1 has no active cart"));
    }

    @Test
    void addItemPassesTheProductAndQuantity() throws Exception {
        Buyer buyer = buyer();
        ShoppingCart cart = new ShoppingCart("CART-ab12cd34", buyer);
        cart.addLine(product("P-1", 49900), 3);
        when(buyerPort.addActiveCartLine(any(), any(), eq(3))).thenReturn(cart);

        mockMvc.perform(post("/api/buyer/cart/items").header(authorization(), bearer(buyer))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":\"P-1\",\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("CART-ab12cd34"))
                .andExpect(jsonPath("$.lines[0].productId").value("P-1"));

        ArgumentCaptor<Product> product = ArgumentCaptor.forClass(Product.class);
        verify(buyerPort).addActiveCartLine(any(), product.capture(), eq(3));
        assertEquals("P-1", product.getValue().getIdentifier());
    }

    @Test
    void updateAndRemoveItemTargetTheProductInThePath() throws Exception {
        Buyer buyer = buyer();
        when(buyerPort.updateActiveCartLineQuantity(any(), any(), eq(5))).thenReturn(new ShoppingCart("C-1", buyer));
        when(buyerPort.removeActiveCartLine(any(), any())).thenReturn(new ShoppingCart("C-1", buyer));

        mockMvc.perform(put("/api/buyer/cart/items/P-7").header(authorization(), bearer(buyer))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":5}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/buyer/cart/items/P-7").header(authorization(), bearer(buyer)))
                .andExpect(status().isOk());

        ArgumentCaptor<Product> updated = ArgumentCaptor.forClass(Product.class);
        ArgumentCaptor<Product> removed = ArgumentCaptor.forClass(Product.class);
        verify(buyerPort).updateActiveCartLineQuantity(any(), updated.capture(), eq(5));
        verify(buyerPort).removeActiveCartLine(any(), removed.capture());
        assertEquals("P-7", updated.getValue().getIdentifier());
        assertEquals("P-7", removed.getValue().getIdentifier());
    }

    @Test
    void confirmActiveCartWithoutBodyReturnsTheGeneratedIdentifierAndTotal() throws Exception {
        Buyer buyer = buyer();
        when(buyerPort.confirmActiveCart(any(), any())).thenReturn(order("ORD-ab12cd34", buyer));

        mockMvc.perform(post("/api/buyer/orders").header(authorization(), bearer(buyer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identifier").value("ORD-ab12cd34"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.total.amount").value(100800))
                .andExpect(jsonPath("$.total.currency").value("COP"));

        ArgumentCaptor<Order> newOrder = ArgumentCaptor.forClass(Order.class);
        verify(buyerPort).confirmActiveCart(any(), newOrder.capture());
        assertNull(newOrder.getValue().getIdentifier());
    }

    @Test
    void confirmActiveCartPassesARequestedIdentifier() throws Exception {
        when(buyerPort.confirmActiveCart(any(), any())).thenReturn(order("O-9", buyer()));

        mockMvc.perform(post("/api/buyer/orders").header(authorization(), bearer(buyer()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"orderId\":\"O-9\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<Order> newOrder = ArgumentCaptor.forClass(Order.class);
        verify(buyerPort).confirmActiveCart(any(), newOrder.capture());
        assertEquals("O-9", newOrder.getValue().getIdentifier());
    }

    @Test
    void takenOrderIdentifierAnswers409() throws Exception {
        when(buyerPort.confirmActiveCart(any(), any()))
                .thenThrow(new DuplicateResourceException("An order with id O-1 already exists"));

        mockMvc.perform(post("/api/buyer/orders").header(authorization(), bearer(buyer()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"orderId\":\"O-1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("An order with id O-1 already exists"));
    }

    @Test
    void blankIdentifierAnswers400() throws Exception {
        mockMvc.perform(post("/api/buyer/orders").header(authorization(), bearer(buyer()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"orderId\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/buyer/carts").header(authorization(), bearer(buyer()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cartId\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(buyerPort);
    }

    @Test
    void createCartWithoutBodyLetsTheServerGenerateTheIdentifier() throws Exception {
        Buyer buyer = buyer();
        when(buyerPort.createShoppingCart(any(), any())).thenReturn(new ShoppingCart("CART-ab12cd34", buyer));

        mockMvc.perform(post("/api/buyer/carts").header(authorization(), bearer(buyer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identifier").value("CART-ab12cd34"));

        ArgumentCaptor<ShoppingCart> cart = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(buyerPort).createShoppingCart(any(), cart.capture());
        assertNull(cart.getValue().getIdentifier());
    }

    @Test
    void productAndWarehouseIdentifiersAreOptional() throws Exception {
        when(sellerPort.publishProduct(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        when(sellerPort.registerWarehouse(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        mockMvc.perform(post("/api/seller/products").header(authorization(), bearer(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mug\",\"productType\":\"PHYSICAL\","
                                + "\"price\":{\"amount\":15000,\"currency\":\"COP\"}}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/seller/warehouses").header(authorization(), bearer(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"North\",\"address\":\"3 Dock Street\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<Product> product = ArgumentCaptor.forClass(Product.class);
        ArgumentCaptor<Warehouse> warehouse = ArgumentCaptor.forClass(Warehouse.class);
        verify(sellerPort).publishProduct(any(), product.capture());
        verify(sellerPort).registerWarehouse(any(), warehouse.capture());
        assertNull(product.getValue().getIdentifier());
        assertNull(warehouse.getValue().getIdentifier());
    }

    @Test
    void activeCartRoutesAreForBuyersOnly() throws Exception {
        String sellerToken = bearer(seller());
        for (MockHttpServletRequestBuilder request : List.of(get("/api/buyer/cart"),
                post("/api/buyer/cart/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"P-1\",\"quantity\":1}"),
                put("/api/buyer/cart/items/P-1").contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"),
                delete("/api/buyer/cart/items/P-1"),
                post("/api/buyer/orders"))) {
            mockMvc.perform(request.header(authorization(), sellerToken)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/buyer/cart")).andExpect(status().isUnauthorized());
        verifyNoInteractions(buyerPort);
    }

    private static Product product(String identifier, long price) {
        return new Product(identifier, "Item " + identifier, ProductType.PHYSICAL, ProductStatus.PUBLISHED, null,
                Money.of(BigDecimal.valueOf(price), Currency.COP), List.of());
    }

    /** Two lines: 2 × 49 900 + 1 × 1 000 = 100 800 COP. */
    private static Order order(String identifier, Buyer buyer) {
        Warehouse warehouse = new Warehouse("WH-1", "North Warehouse", "1 Main Street", WarehouseOwnerType.MARKETPLACE);
        Product shirt = product("P-1", 49900);
        Product sticker = product("P-2", 1000);
        return new Order(identifier, buyer, OrderStatus.PENDING_PAYMENT, List.of(
                new OrderLine(shirt, shirt.getName(), 2, shirt.getPrice(), warehouse),
                new OrderLine(sticker, sticker.getName(), 1, sticker.getPrice(), warehouse)), LocalDateTime.now());
    }
}
