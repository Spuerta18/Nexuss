package nexussMarket.adapters.in.rest.controllers;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import nexussMarket.adapters.in.rest.mappers.CatalogRestMapper;
import nexussMarket.adapters.in.rest.mappers.DomainReferences;
import nexussMarket.adapters.in.rest.mappers.InventoryRestMapper;
import nexussMarket.adapters.in.rest.mappers.OrderRestMapper;
import nexussMarket.adapters.in.rest.mappers.UserRestMapper;
import nexussMarket.adapters.in.rest.responses.InventoryItemResponse;
import nexussMarket.adapters.in.rest.responses.OrderResponse;
import nexussMarket.adapters.in.rest.responses.ProductResponse;
import nexussMarket.adapters.in.rest.responses.UserResponse;
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
}
