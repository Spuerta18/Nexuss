package nexussMarket.adapters.in.rest.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

import nexussMarket.domain.exceptions.SellerNotAuthorizedException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

class SellerRestControllerTest extends RestControllerTestSupport {

    private static final String PRODUCT = "{\"identifier\":\"P-1\",\"name\":\"T-shirt\",\"productType\":\"PHYSICAL\","
            + "\"price\":{\"amount\":49900.00,\"currency\":\"COP\"},"
            + "\"variants\":[{\"attribute\":\"SIZE\",\"value\":\"M\"}]}";

    @Test
    void publishProductMapsTheRequestAndAnswers201() throws Exception {
        Seller seller = seller();
        when(sellerPort.publishProduct(any(), any())).thenAnswer(invocation -> {
            Product p = invocation.getArgument(1);
            return new Product(p.getIdentifier(), p.getName(), p.getProductType(), ProductStatus.PUBLISHED, seller,
                    p.getPrice(), p.getVariants());
        });

        mockMvc.perform(post("/api/seller/products").header(authorization(), bearer(seller))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identifier").value("P-1"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.sellerId").value("S-1"))
                .andExpect(jsonPath("$.price.currency").value("COP"))
                .andExpect(jsonPath("$.variants[0].attribute").value("SIZE"));

        ArgumentCaptor<Product> product = ArgumentCaptor.forClass(Product.class);
        verify(sellerPort).publishProduct(any(), product.capture());
        assertEquals(Money.of(new BigDecimal("49900"), Currency.COP), product.getValue().getPrice());
        assertEquals(ProductType.PHYSICAL, product.getValue().getProductType());
    }

    @Test
    void registerWarehouseIsAlwaysSellerOwned() throws Exception {
        when(sellerPort.registerWarehouse(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        mockMvc.perform(post("/api/seller/warehouses").header(authorization(), bearer(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"WH-9\",\"name\":\"North Warehouse\",\"address\":\"1 Main Street\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerType").value("SELLER"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void inventoryInboundReturnsTheStock() throws Exception {
        Product product = new Product("P-1", "T-shirt", ProductType.PHYSICAL, ProductStatus.PUBLISHED);
        Warehouse warehouse = new Warehouse("WH-1", "North Warehouse", "1 Main Street", WarehouseOwnerType.SELLER);
        when(sellerPort.registerInventoryInbound(any(), any(), any(), eq(5)))
                .thenReturn(new InventoryItem(product, warehouse, 5, 0, 0));

        mockMvc.perform(post("/api/seller/inventory/inbound").header(authorization(), bearer(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"P-1\",\"warehouseId\":\"WH-1\",\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(5));
    }

    @Test
    void withoutTokenAnswers401() throws Exception {
        mockMvc.perform(post("/api/seller/products").contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(sellerPort);
    }

    @Test
    void anotherRoleAnswers403() throws Exception {
        mockMvc.perform(post("/api/seller/products").header(authorization(), bearer(buyer()))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isForbidden());
        verifyNoInteractions(sellerPort);
    }

    @Test
    void invalidProductAnswers400() throws Exception {
        mockMvc.perform(post("/api/seller/products").header(authorization(), bearer(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"P-1\",\"name\":\"\",\"productType\":\"PHYSICAL\","
                                + "\"price\":{\"amount\":-1,\"currency\":\"COP\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'price.amount')]").exists());
        verifyNoInteractions(sellerPort);
    }

    @Test
    void unknownCodesAnswer400() throws Exception {
        String token = bearer(seller());
        mockMvc.perform(post("/api/seller/products").header(authorization(), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT.replace("\"COP\"", "\"XYZ\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown currency 'XYZ'"));
        mockMvc.perform(patch("/api/seller/products/P-1/status").header(authorization(), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"GONE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown product status 'GONE'"));
        verifyNoInteractions(sellerPort);
    }

    @Test
    void sellerNotAuthorizedAnswers403() throws Exception {
        when(sellerPort.publishProduct(any(), any()))
                .thenThrow(new SellerNotAuthorizedException("Seller S-1 is not allowed to publish products"));

        mockMvc.perform(post("/api/seller/products").header(authorization(), bearer(seller()))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Seller S-1 is not allowed to publish products"));
    }
}
