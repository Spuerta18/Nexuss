package nexussMarket.adapters.in.rest.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

class SupervisorRestControllerTest extends RestControllerTestSupport {

    private static final Product PRODUCT = new Product("P-1", "T-shirt", ProductType.PHYSICAL,
            ProductStatus.PUBLISHED, null, Money.of(new BigDecimal("49900"), Currency.COP), List.of());

    @Test
    void listsPublishedProducts() throws Exception {
        when(supervisorPort.listPublishedProducts(any())).thenReturn(List.of(PRODUCT));

        mockMvc.perform(get("/api/supervisor/products").header(authorization(), bearer(supervisor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].identifier").value("P-1"))
                .andExpect(jsonPath("$[0].price.amount").value(49900));
    }

    @Test
    void consultsInventory() throws Exception {
        Warehouse warehouse = new Warehouse("WH-1", "North Warehouse", "1 Main Street", WarehouseOwnerType.MARKETPLACE);
        when(supervisorPort.consultInventory(any(), any()))
                .thenReturn(List.of(new InventoryItem(PRODUCT, warehouse, 7, 2, 1)));

        mockMvc.perform(get("/api/supervisor/products/P-1/inventory").header(authorization(), bearer(supervisor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].warehouseId").value("WH-1"))
                .andExpect(jsonPath("$[0].reservedQuantity").value(2));
    }

    @Test
    void consultsAUserByRoleAndIdentifier() throws Exception {
        when(supervisorPort.consultUser(any(), any())).thenReturn(seller());

        mockMvc.perform(get("/api/supervisor/users/S-1").param("role", "SELLER")
                        .header(authorization(), bearer(supervisor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void withoutTokenAnswers401() throws Exception {
        mockMvc.perform(get("/api/supervisor/products"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(supervisorPort);
    }

    @Test
    void anotherRoleAnswers403() throws Exception {
        mockMvc.perform(get("/api/supervisor/products").header(authorization(), bearer(seller())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(supervisorPort);
    }

    @Test
    void missingRoleParameterAnswers400() throws Exception {
        mockMvc.perform(get("/api/supervisor/users/S-1").header(authorization(), bearer(supervisor())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(supervisorPort);
    }

    @Test
    void writeMethodsDoNotExistForSupervisors() throws Exception {
        mockMvc.perform(post("/api/supervisor/products").header(authorization(), bearer(supervisor())))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void domainAuthorizationFailureAnswers403() throws Exception {
        when(supervisorPort.consultOrder(any(), any()))
                .thenThrow(new OperationNotAllowedException("User V-1 is not allowed to access order O-1"));

        mockMvc.perform(get("/api/supervisor/orders/O-1").header(authorization(), bearer(supervisor())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User V-1 is not allowed to access order O-1"));
    }
}
