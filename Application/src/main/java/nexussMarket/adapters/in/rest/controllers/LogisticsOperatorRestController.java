package nexussMarket.adapters.in.rest.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nexussMarket.adapters.in.rest.mappers.DomainReferences;
import nexussMarket.adapters.in.rest.mappers.InventoryRestMapper;
import nexussMarket.adapters.in.rest.mappers.OrderRestMapper;
import nexussMarket.adapters.in.rest.mappers.ShipmentRestMapper;
import nexussMarket.adapters.in.rest.requests.AdvanceOrderStatusRequest;
import nexussMarket.adapters.in.rest.requests.ChangePasswordRequest;
import nexussMarket.adapters.in.rest.requests.CreateShipmentRequest;
import nexussMarket.adapters.in.rest.requests.InventoryMovementRequest;
import nexussMarket.adapters.in.rest.responses.InventoryItemResponse;
import nexussMarket.adapters.in.rest.responses.OrderResponse;
import nexussMarket.adapters.in.rest.responses.ShipmentResponse;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.LogisticsOperatorPort;

/**
 * Endpoints of the {@code LOGISTICS_OPERATOR} role ({@link LogisticsOperatorPort}):
 * order payment, the shipment lifecycle and warehouse stock.
 */
@RestController
@RequestMapping("/api/logistics")
@PreAuthorize("hasRole('LOGISTICS_OPERATOR')")
@RequiredArgsConstructor
public class LogisticsOperatorRestController {

    private final LogisticsOperatorPort logisticsOperatorPort;

    @GetMapping("/orders/{orderId}")
    public OrderResponse consultOrder(@AuthenticationPrincipal(expression = "user") User actor,
                                      @PathVariable String orderId) {
        return OrderRestMapper.toResponse(logisticsOperatorPort.consultOrder(actor, DomainReferences.order(orderId)));
    }

    @PostMapping("/orders/{orderId}/status")
    public OrderResponse advanceOrderStatus(@AuthenticationPrincipal(expression = "user") User actor,
                                            @PathVariable String orderId,
                                            @Valid @RequestBody AdvanceOrderStatusRequest request) {
        return OrderRestMapper.toResponse(logisticsOperatorPort.advanceOrderStatus(actor,
                DomainReferences.order(orderId), OrderRestMapper.orderStatus(request.targetStatus())));
    }

    @PostMapping("/shipments")
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentResponse createShipment(@AuthenticationPrincipal(expression = "user") User actor,
                                           @Valid @RequestBody CreateShipmentRequest request) {
        return ShipmentRestMapper.toResponse(
                logisticsOperatorPort.createShipment(actor, ShipmentRestMapper.newShipment(request)));
    }

    @PostMapping("/shipments/{shipmentId}/pack")
    public ShipmentResponse packShipment(@AuthenticationPrincipal(expression = "user") User actor,
                                         @PathVariable String shipmentId) {
        return ShipmentRestMapper.toResponse(
                logisticsOperatorPort.packShipment(actor, DomainReferences.shipment(shipmentId)));
    }

    @PostMapping("/shipments/{shipmentId}/dispatch")
    public ShipmentResponse dispatchShipment(@AuthenticationPrincipal(expression = "user") User actor,
                                             @PathVariable String shipmentId) {
        return ShipmentRestMapper.toResponse(
                logisticsOperatorPort.dispatchShipment(actor, DomainReferences.shipment(shipmentId)));
    }

    @PostMapping("/shipments/{shipmentId}/deliver")
    public ShipmentResponse confirmDelivery(@AuthenticationPrincipal(expression = "user") User actor,
                                            @PathVariable String shipmentId) {
        return ShipmentRestMapper.toResponse(
                logisticsOperatorPort.confirmDelivery(actor, DomainReferences.shipment(shipmentId)));
    }

    @PostMapping("/inventory/damaged")
    public InventoryItemResponse reportDamagedInventory(@AuthenticationPrincipal(expression = "user") User actor,
                                                        @Valid @RequestBody InventoryMovementRequest request) {
        return InventoryRestMapper.toResponse(logisticsOperatorPort.reportDamagedInventory(actor,
                DomainReferences.product(request.productId()), DomainReferences.warehouse(request.warehouseId()),
                request.quantity()));
    }

    @PostMapping("/inventory/returns")
    public InventoryItemResponse registerInventoryReturn(@AuthenticationPrincipal(expression = "user") User actor,
                                                         @Valid @RequestBody InventoryMovementRequest request) {
        return InventoryRestMapper.toResponse(logisticsOperatorPort.registerInventoryReturn(actor,
                DomainReferences.product(request.productId()), DomainReferences.warehouse(request.warehouseId()),
                request.quantity()));
    }

    @GetMapping("/products/{productId}/inventory")
    public List<InventoryItemResponse> consultInventory(@AuthenticationPrincipal(expression = "user") User actor,
                                                        @PathVariable String productId) {
        return InventoryRestMapper.toResponses(
                logisticsOperatorPort.consultInventory(actor, DomainReferences.product(productId)));
    }

    /** Revokes every token of the caller, including the one used for this request. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal(expression = "user") User actor) {
        logisticsOperatorPort.logout(actor);
    }

    /** Changes the caller's password; every token issued before, including this one, stops working. */
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal(expression = "user") User actor,
                               @Valid @RequestBody ChangePasswordRequest request) {
        logisticsOperatorPort.changePassword(actor, request.currentPassword(), request.newPassword());
    }
}
