package nexussMarket.adapters.in.rest.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nexussMarket.adapters.in.rest.mappers.CartRestMapper;
import nexussMarket.adapters.in.rest.mappers.CatalogRestMapper;
import nexussMarket.adapters.in.rest.mappers.DomainReferences;
import nexussMarket.adapters.in.rest.mappers.InvoiceRestMapper;
import nexussMarket.adapters.in.rest.mappers.OrderRestMapper;
import nexussMarket.adapters.in.rest.mappers.ReturnRestMapper;
import nexussMarket.adapters.in.rest.mappers.ShipmentRestMapper;
import nexussMarket.adapters.in.rest.requests.AddCartLineRequest;
import nexussMarket.adapters.in.rest.requests.ChangePasswordRequest;
import nexussMarket.adapters.in.rest.requests.ConfirmOrderRequest;
import nexussMarket.adapters.in.rest.requests.CreateCartRequest;
import nexussMarket.adapters.in.rest.requests.QuantityRequest;
import nexussMarket.adapters.in.rest.requests.RequestReturnRequest;
import nexussMarket.adapters.in.rest.responses.CartResponse;
import nexussMarket.adapters.in.rest.responses.InvoiceResponse;
import nexussMarket.adapters.in.rest.responses.OrderResponse;
import nexussMarket.adapters.in.rest.responses.ProductResponse;
import nexussMarket.adapters.in.rest.responses.ReturnRequestResponse;
import nexussMarket.adapters.in.rest.responses.ShipmentResponse;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.BuyerPort;

/** Endpoints of the {@code BUYER} role ({@link BuyerPort}): carts, orders, catalog browsing and returns. */
@RestController
@RequestMapping("/api/buyer")
@PreAuthorize("hasRole('BUYER')")
@RequiredArgsConstructor
public class BuyerRestController {

    private final BuyerPort buyerPort;

    /** The body, and the cart identifier in it, are optional: without them the server generates the identifier. */
    @PostMapping("/carts")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse createCart(@AuthenticationPrincipal(expression = "user") User actor,
                                   @Valid @RequestBody(required = false) CreateCartRequest request) {
        return CartRestMapper.toResponse(buyerPort.createShoppingCart(actor, CartRestMapper.newCart(request)));
    }

    @GetMapping("/carts/{cartId}")
    public CartResponse consultCart(@AuthenticationPrincipal(expression = "user") User actor,
                                    @PathVariable String cartId) {
        return CartRestMapper.toResponse(buyerPort.consultCart(actor, DomainReferences.cart(cartId)));
    }

    @PostMapping("/carts/{cartId}/lines")
    public CartResponse addCartLine(@AuthenticationPrincipal(expression = "user") User actor,
                                    @PathVariable String cartId, @Valid @RequestBody AddCartLineRequest request) {
        return CartRestMapper.toResponse(buyerPort.addCartLine(actor, DomainReferences.cart(cartId),
                DomainReferences.product(request.productId()), request.quantity()));
    }

    @PutMapping("/carts/{cartId}/lines/{productId}")
    public CartResponse updateCartLineQuantity(@AuthenticationPrincipal(expression = "user") User actor,
                                               @PathVariable String cartId, @PathVariable String productId,
                                               @Valid @RequestBody QuantityRequest request) {
        return CartRestMapper.toResponse(buyerPort.updateCartLineQuantity(actor, DomainReferences.cart(cartId),
                DomainReferences.product(productId), request.quantity()));
    }

    @DeleteMapping("/carts/{cartId}/lines/{productId}")
    public CartResponse removeCartLine(@AuthenticationPrincipal(expression = "user") User actor,
                                       @PathVariable String cartId, @PathVariable String productId) {
        return CartRestMapper.toResponse(buyerPort.removeCartLine(actor, DomainReferences.cart(cartId),
                DomainReferences.product(productId)));
    }

    /** The body, and the order identifier in it, are optional: without them the server generates the identifier. */
    @PostMapping("/carts/{cartId}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse confirmOrder(@AuthenticationPrincipal(expression = "user") User actor,
                                      @PathVariable String cartId,
                                      @Valid @RequestBody(required = false) ConfirmOrderRequest request) {
        return OrderRestMapper.toResponse(buyerPort.confirmOrder(actor, DomainReferences.cart(cartId),
                OrderRestMapper.newOrder(request)));
    }

    /** The authenticated buyer's active cart; 404 when there is none. */
    @GetMapping("/cart")
    public CartResponse consultActiveCart(@AuthenticationPrincipal(expression = "user") User actor) {
        return CartRestMapper.toResponse(buyerPort.consultActiveCart(actor));
    }

    /** Adds a line to the active cart, creating that cart first when the buyer has none. */
    @PostMapping("/cart/items")
    public CartResponse addActiveCartLine(@AuthenticationPrincipal(expression = "user") User actor,
                                          @Valid @RequestBody AddCartLineRequest request) {
        return CartRestMapper.toResponse(buyerPort.addActiveCartLine(actor,
                DomainReferences.product(request.productId()), request.quantity()));
    }

    @PutMapping("/cart/items/{productId}")
    public CartResponse updateActiveCartLineQuantity(@AuthenticationPrincipal(expression = "user") User actor,
                                                     @PathVariable String productId,
                                                     @Valid @RequestBody QuantityRequest request) {
        return CartRestMapper.toResponse(buyerPort.updateActiveCartLineQuantity(actor,
                DomainReferences.product(productId), request.quantity()));
    }

    @DeleteMapping("/cart/items/{productId}")
    public CartResponse removeActiveCartLine(@AuthenticationPrincipal(expression = "user") User actor,
                                             @PathVariable String productId) {
        return CartRestMapper.toResponse(buyerPort.removeActiveCartLine(actor, DomainReferences.product(productId)));
    }

    /** Confirms the active cart; the body, and the order identifier in it, are optional. */
    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse confirmActiveCart(@AuthenticationPrincipal(expression = "user") User actor,
                                           @Valid @RequestBody(required = false) ConfirmOrderRequest request) {
        return OrderRestMapper.toResponse(buyerPort.confirmActiveCart(actor, OrderRestMapper.newOrder(request)));
    }

    /** The orders of the authenticated buyer. */
    @GetMapping("/orders")
    public List<OrderResponse> listOrders(@AuthenticationPrincipal(expression = "user") User actor) {
        return OrderRestMapper.toResponses(
                buyerPort.listBuyerOrders(actor, DomainReferences.buyer(actor.getIdentifier())));
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse consultOrder(@AuthenticationPrincipal(expression = "user") User actor,
                                      @PathVariable String orderId) {
        return OrderRestMapper.toResponse(buyerPort.consultOrder(actor, DomainReferences.order(orderId)));
    }

    @PostMapping("/orders/{orderId}/cancel")
    public OrderResponse cancelOrder(@AuthenticationPrincipal(expression = "user") User actor,
                                     @PathVariable String orderId) {
        return OrderRestMapper.toResponse(buyerPort.cancelOrder(actor, DomainReferences.order(orderId)));
    }

    @PostMapping("/orders/{orderId}/returns")
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnRequestResponse requestReturn(@AuthenticationPrincipal(expression = "user") User actor,
                                               @PathVariable String orderId,
                                               @Valid @RequestBody RequestReturnRequest request) {
        return ReturnRestMapper.toResponse(
                buyerPort.requestReturn(actor, ReturnRestMapper.newReturnRequest(orderId, request)));
    }

    @GetMapping("/products")
    public List<ProductResponse> listPublishedProducts(@AuthenticationPrincipal(expression = "user") User actor) {
        return CatalogRestMapper.toResponses(buyerPort.listPublishedProducts(actor));
    }

    @GetMapping("/products/{productId}")
    public ProductResponse consultProduct(@AuthenticationPrincipal(expression = "user") User actor,
                                          @PathVariable String productId) {
        return CatalogRestMapper.toResponse(buyerPort.consultProduct(actor, DomainReferences.product(productId)));
    }

    /** The invoice of the order, issued when it was paid; 404 until then. Only for the buyer who placed the order. */
    @GetMapping("/orders/{orderId}/invoice")
    public InvoiceResponse consultOrderInvoice(@AuthenticationPrincipal(expression = "user") User actor,
                                              @PathVariable String orderId) {
        return InvoiceRestMapper.toResponse(buyerPort.consultOrderInvoice(actor, DomainReferences.order(orderId)));
    }

    /** The shipment of the order; 404 until it is created. Only for the buyer who placed the order. */
    @GetMapping("/orders/{orderId}/shipment")
    public ShipmentResponse consultOrderShipment(@AuthenticationPrincipal(expression = "user") User actor,
                                                 @PathVariable String orderId) {
        return ShipmentRestMapper.toResponse(buyerPort.consultOrderShipment(actor, DomainReferences.order(orderId)));
    }

    /** Revokes every token of the caller, including the one used for this request. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal(expression = "user") User actor) {
        buyerPort.logout(actor);
    }

    /** Changes the caller's password; every token issued before, including this one, stops working. */
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal(expression = "user") User actor,
                               @Valid @RequestBody ChangePasswordRequest request) {
        buyerPort.changePassword(actor, request.currentPassword(), request.newPassword());
    }
}
