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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nexussMarket.adapters.in.rest.mappers.AuditRestMapper;
import nexussMarket.adapters.in.rest.mappers.CatalogRestMapper;
import nexussMarket.adapters.in.rest.mappers.DomainReferences;
import nexussMarket.adapters.in.rest.mappers.InventoryRestMapper;
import nexussMarket.adapters.in.rest.mappers.InvoiceRestMapper;
import nexussMarket.adapters.in.rest.mappers.OrderRestMapper;
import nexussMarket.adapters.in.rest.mappers.PageRestMapper;
import nexussMarket.adapters.in.rest.mappers.UserRestMapper;
import nexussMarket.adapters.in.rest.mappers.WarehouseRestMapper;
import nexussMarket.adapters.in.rest.requests.ChangePasswordRequest;
import nexussMarket.adapters.in.rest.responses.AuditLogResponse;
import nexussMarket.adapters.in.rest.responses.InventoryItemResponse;
import nexussMarket.adapters.in.rest.responses.InvoiceResponse;
import nexussMarket.adapters.in.rest.responses.OperationResponse;
import nexussMarket.adapters.in.rest.responses.OrderResponse;
import nexussMarket.adapters.in.rest.responses.PageResponse;
import nexussMarket.adapters.in.rest.responses.ProductResponse;
import nexussMarket.adapters.in.rest.responses.UserResponse;
import nexussMarket.adapters.in.rest.responses.WarehouseResponse;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.SupervisorPort;

/** Read-only endpoints of the {@code SUPERVISOR} role ({@link SupervisorPort}). */
@RestController
@RequestMapping("/api/supervisor")
@PreAuthorize("hasRole('SUPERVISOR')")
@RequiredArgsConstructor
public class SupervisorRestController {

    private final SupervisorPort supervisorPort;

    @GetMapping("/orders/{orderId}")
    public OrderResponse consultOrder(@AuthenticationPrincipal(expression = "user") User actor,
                                      @PathVariable String orderId) {
        return OrderRestMapper.toResponse(supervisorPort.consultOrder(actor, DomainReferences.order(orderId)));
    }

    @GetMapping("/products")
    public List<ProductResponse> listPublishedProducts(@AuthenticationPrincipal(expression = "user") User actor) {
        return CatalogRestMapper.toResponses(supervisorPort.listPublishedProducts(actor));
    }

    @GetMapping("/products/{productId}")
    public ProductResponse consultProduct(@AuthenticationPrincipal(expression = "user") User actor,
                                          @PathVariable String productId) {
        return CatalogRestMapper.toResponse(supervisorPort.consultProduct(actor, DomainReferences.product(productId)));
    }

    @GetMapping("/products/{productId}/inventory")
    public List<InventoryItemResponse> consultInventory(@AuthenticationPrincipal(expression = "user") User actor,
                                                        @PathVariable String productId) {
        return InventoryRestMapper.toResponses(
                supervisorPort.consultInventory(actor, DomainReferences.product(productId)));
    }

    /** {@code role} is the role of the user consulted: needed to build a reference of the right subtype. */
    @GetMapping("/users/{userId}")
    public UserResponse consultUser(@AuthenticationPrincipal(expression = "user") User actor,
                                    @PathVariable String userId, @RequestParam String role) {
        return UserRestMapper.toResponse(
                supervisorPort.consultUser(actor, DomainReferences.user(userId, UserRestMapper.role(role))));
    }

    @GetMapping("/operations")
    public List<OperationResponse> consultOperations(@AuthenticationPrincipal(expression = "user") User actor) {
        return AuditRestMapper.toOperationResponses(supervisorPort.consultOperations(actor));
    }

    @GetMapping("/audit-logs")
    public List<AuditLogResponse> consultAuditLogs(@AuthenticationPrincipal(expression = "user") User actor) {
        return AuditRestMapper.toAuditLogResponses(supervisorPort.consultAuditLogs(actor));
    }

    /** The invoice of the order, issued when it was paid; 404 until then. */
    @GetMapping("/orders/{orderId}/invoice")
    public InvoiceResponse consultOrderInvoice(@AuthenticationPrincipal(expression = "user") User actor,
                                              @PathVariable String orderId) {
        return InvoiceRestMapper.toResponse(supervisorPort.consultOrderInvoice(actor, DomainReferences.order(orderId)));
    }

    /** Revokes every token of the caller, including the one used for this request. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal(expression = "user") User actor) {
        supervisorPort.logout(actor);
    }

    /** Changes the caller's password; every token issued before, including this one, stops working. */
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal(expression = "user") User actor,
                               @Valid @RequestBody ChangePasswordRequest request) {
        supervisorPort.changePassword(actor, request.currentPassword(), request.newPassword());
    }

    /** A page of users ({@code ?role=} filters by role; {@code page} from 0, {@code size} 1-100, default 20). */
    @GetMapping("/users")
    public PageResponse<UserResponse> listUsers(@AuthenticationPrincipal(expression = "user") User actor,
                                                @RequestParam(required = false) String role,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return PageRestMapper.toResponse(supervisorPort.listUsers(actor, role != null ? UserRestMapper.role(role) : null,
                PageRestMapper.query(page, size)), UserRestMapper::toResponse);
    }

    /** A page of orders, most recent first ({@code page} from 0, {@code size} 1-100, default 20). */
    @GetMapping("/orders")
    public PageResponse<OrderResponse> listOrders(@AuthenticationPrincipal(expression = "user") User actor,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return PageRestMapper.toResponse(supervisorPort.listOrders(actor, PageRestMapper.query(page, size)),
                OrderRestMapper::toResponse);
    }

    /** A page of warehouses ({@code page} from 0, {@code size} 1-100, default 20). */
    @GetMapping("/warehouses")
    public PageResponse<WarehouseResponse> listWarehouses(@AuthenticationPrincipal(expression = "user") User actor,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return PageRestMapper.toResponse(supervisorPort.listWarehouses(actor, PageRestMapper.query(page, size)),
                WarehouseRestMapper::toResponse);
    }

    /** A page of inventory records ({@code page} from 0, {@code size} 1-100, default 20). */
    @GetMapping("/inventory")
    public PageResponse<InventoryItemResponse> listInventory(@AuthenticationPrincipal(expression = "user") User actor,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return PageRestMapper.toResponse(supervisorPort.listInventory(actor, PageRestMapper.query(page, size)),
                InventoryRestMapper::toResponse);
    }
}
