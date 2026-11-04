package nexussMarket.adapters.in.rest.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.exceptions.ProductInOpenOrderException;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;

/** {@code PUT} and {@code DELETE /api/seller/products/{productId}}. */
class ProductManagementRestControllerTest extends RestControllerTestSupport {

    private static final String BODY = "{\"name\":\"Silent Keyboard\",\"price\":{\"amount\":1500,\"currency\":\"COP\"}}";

    @Test
    void putChangesNameAndPrice() throws Exception {
        when(sellerPort.updateProduct(any(), argThat(changes -> "P-1".equals(changes.getIdentifier())
                && "Silent Keyboard".equals(changes.getName()))))
                .thenReturn(product(ProductStatus.PUBLISHED, "Silent Keyboard", 1500));

        mockMvc.perform(put("/api/seller/products/P-1").header(authorization(), bearer(seller()))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Silent Keyboard"))
                .andExpect(jsonPath("$.price.amount").value(1500));
    }

    @Test
    void putRejectsABlankNameOrANonPositivePrice() throws Exception {
        mockMvc.perform(put("/api/seller/products/P-1").header(authorization(), bearer(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"price\":{\"amount\":1500,\"currency\":\"COP\"}}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/seller/products/P-1").header(authorization(), bearer(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Free\",\"price\":{\"amount\":0,\"currency\":\"COP\"}}"))
                .andExpect(status().isBadRequest());
        verify(sellerPort, never()).updateProduct(any(), any());
    }

    @Test
    void deleteAnswersTheDiscontinuedProduct() throws Exception {
        when(sellerPort.discontinueProduct(any(), any())).thenReturn(product(ProductStatus.DISCONTINUED, "Keyboard", 1000));

        mockMvc.perform(delete("/api/seller/products/P-1").header(authorization(), bearer(seller())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISCONTINUED"));
    }

    @Test
    void deleteIs409InAnOpenOrderAnd403ForAnotherSeller() throws Exception {
        when(sellerPort.discontinueProduct(any(), any()))
                .thenThrow(new ProductInOpenOrderException("open order"))
                .thenThrow(new OperationNotAllowedException("not yours"));

        mockMvc.perform(delete("/api/seller/products/P-1").header(authorization(), bearer(seller())))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/seller/products/P-1").header(authorization(), bearer(seller())))
                .andExpect(status().isForbidden());
    }

    @Test
    void otherRolesCannotEditNorDelete() throws Exception {
        mockMvc.perform(put("/api/seller/products/P-1").header(authorization(), bearer(buyer()))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/seller/products/P-1").header(authorization(), bearer(administrator())))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/seller/products/P-1"))
                .andExpect(status().isUnauthorized());
        verify(sellerPort, never()).discontinueProduct(any(), any());
    }

    private static Product product(ProductStatus status, String name, long price) {
        return new Product("P-1", name, ProductType.PHYSICAL, status, seller(),
                Money.of(BigDecimal.valueOf(price), Currency.COP), List.of());
    }
}
