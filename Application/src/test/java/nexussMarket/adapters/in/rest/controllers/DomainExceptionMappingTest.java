package nexussMarket.adapters.in.rest.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import nexussMarket.domain.exceptions.BuyerNotEnabledException;
import nexussMarket.domain.exceptions.CurrencyMismatchException;
import nexussMarket.domain.exceptions.DomainException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.exceptions.EmptyCartException;
import nexussMarket.domain.exceptions.EmptyOrderException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.InactiveWarehouseException;
import nexussMarket.domain.exceptions.IncorrectCurrentPasswordException;
import nexussMarket.domain.exceptions.InsufficientStockException;
import nexussMarket.domain.exceptions.InvalidMoneyException;
import nexussMarket.domain.exceptions.InvalidOrderStatusTransitionException;
import nexussMarket.domain.exceptions.InvalidPasswordException;
import nexussMarket.domain.exceptions.InvalidProductPriceException;
import nexussMarket.domain.exceptions.InvalidQuantityException;
import nexussMarket.domain.exceptions.InvalidShipmentStatusTransitionException;
import nexussMarket.domain.exceptions.InventoryNotTrackedException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.exceptions.OrderNotShippableException;
import nexussMarket.domain.exceptions.OrderStatusRequiresShipmentException;
import nexussMarket.domain.exceptions.ProductNotPurchasableException;
import nexussMarket.domain.exceptions.RefundAlreadyProcessedException;
import nexussMarket.domain.exceptions.ReturnAlreadyDecidedException;
import nexussMarket.domain.exceptions.ReturnNotEligibleException;
import nexussMarket.domain.exceptions.SellerNotAuthorizedException;

/** {@code GlobalExceptionHandler} answers each domain exception with its HTTP status and its own message. */
class DomainExceptionMappingTest extends RestControllerTestSupport {

    static Stream<Arguments> exceptions() {
        return Stream.of(
                Arguments.of(new EntityNotFoundException("missing"), 404),
                // 409: the current state of the data does not allow the operation.
                Arguments.of(new DuplicateResourceException("duplicate"), 409),
                Arguments.of(new InsufficientStockException("no stock"), 409),
                Arguments.of(new InvalidOrderStatusTransitionException("order transition"), 409),
                Arguments.of(new InvalidShipmentStatusTransitionException("shipment transition"), 409),
                Arguments.of(new EmptyCartException("empty cart"), 409),
                Arguments.of(new ProductNotPurchasableException("not purchasable"), 409),
                Arguments.of(new CurrencyMismatchException("currency"), 409),
                Arguments.of(new OrderNotShippableException("not paid"), 409),
                Arguments.of(new EmptyOrderException("no lines"), 409),
                Arguments.of(new ReturnNotEligibleException("not delivered"), 409),
                Arguments.of(new ReturnAlreadyDecidedException("decided"), 409),
                Arguments.of(new RefundAlreadyProcessedException("processed"), 409),
                Arguments.of(new InactiveWarehouseException("inactive"), 409),
                Arguments.of(new InventoryNotTrackedException("digital"), 409),
                // 403: the actor may not perform the operation.
                Arguments.of(new OperationNotAllowedException("not allowed"), 403),
                Arguments.of(new SellerNotAuthorizedException("seller suspended"), 403),
                Arguments.of(new BuyerNotEnabledException("buyer suspended"), 403),
                // 400: a value sent by the client breaks a rule.
                Arguments.of(new InvalidQuantityException("quantity"), 400),
                Arguments.of(new InvalidProductPriceException("price"), 400),
                Arguments.of(new InvalidMoneyException("money"), 400),
                Arguments.of(new InvalidPasswordException("short password"), 400),
                Arguments.of(new IncorrectCurrentPasswordException("wrong current password"), 400),
                Arguments.of(new OrderStatusRequiresShipmentException("use the shipment"), 400),
                Arguments.of(new DomainException("any other rule"), 400));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("exceptions")
    void mapsTheExceptionToItsStatus(DomainException exception, int expectedStatus) throws Exception {
        when(buyerPort.consultOrder(any(), any())).thenThrow(exception);

        mockMvc.perform(get("/api/buyer/orders/O-1").header(authorization(), bearer(buyer())))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.message").value(exception.getMessage()));
    }
}
