package nexussMarket.adapters.in.rest.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nexussMarket.adapters.in.rest.mappers.CatalogRestMapper;
import nexussMarket.adapters.in.rest.mappers.DomainReferences;
import nexussMarket.adapters.in.rest.mappers.InventoryRestMapper;
import nexussMarket.adapters.in.rest.mappers.WarehouseRestMapper;
import nexussMarket.adapters.in.rest.requests.InventoryAdjustmentRequest;
import nexussMarket.adapters.in.rest.requests.InventoryMovementRequest;
import nexussMarket.adapters.in.rest.requests.ProductStatusRequest;
import nexussMarket.adapters.in.rest.requests.PublishProductRequest;
import nexussMarket.adapters.in.rest.requests.RegisterWarehouseRequest;
import nexussMarket.adapters.in.rest.requests.VariantsRequest;
import nexussMarket.adapters.in.rest.responses.InventoryItemResponse;
import nexussMarket.adapters.in.rest.responses.ProductResponse;
import nexussMarket.adapters.in.rest.responses.WarehouseResponse;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.SellerPort;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/** Endpoints of the {@code SELLER} role ({@link SellerPort}): own catalog, own warehouses and inventory. */
@RestController
@RequestMapping("/api/seller")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
public class SellerRestController {

    private final SellerPort sellerPort;

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse publishProduct(@AuthenticationPrincipal(expression = "user") User actor,
                                          @Valid @RequestBody PublishProductRequest request) {
        return CatalogRestMapper.toResponse(sellerPort.publishProduct(actor, CatalogRestMapper.newProduct(request)));
    }

    @PatchMapping("/products/{productId}/status")
    public ProductResponse updateProductStatus(@AuthenticationPrincipal(expression = "user") User actor,
                                               @PathVariable String productId,
                                               @Valid @RequestBody ProductStatusRequest request) {
        return CatalogRestMapper.toResponse(sellerPort.updateProductStatus(actor, DomainReferences.product(productId),
                CatalogRestMapper.productStatus(request.status())));
    }

    @PutMapping("/products/{productId}/variants")
    public ProductResponse updateProductVariants(@AuthenticationPrincipal(expression = "user") User actor,
                                                 @PathVariable String productId,
                                                 @Valid @RequestBody VariantsRequest request) {
        return CatalogRestMapper.toResponse(sellerPort.updateProductVariants(actor,
                DomainReferences.product(productId), CatalogRestMapper.toVariants(request.variants())));
    }

    /** The products of the authenticated seller, in every status. */
    @GetMapping("/products")
    public List<ProductResponse> listOwnProducts(@AuthenticationPrincipal(expression = "user") User actor) {
        return CatalogRestMapper.toResponses(
                sellerPort.listSellerProducts(actor, DomainReferences.seller(actor.getIdentifier())));
    }

    @GetMapping("/products/{productId}")
    public ProductResponse consultProduct(@AuthenticationPrincipal(expression = "user") User actor,
                                          @PathVariable String productId) {
        return CatalogRestMapper.toResponse(sellerPort.consultProduct(actor, DomainReferences.product(productId)));
    }

    @GetMapping("/products/{productId}/inventory")
    public List<InventoryItemResponse> consultInventory(@AuthenticationPrincipal(expression = "user") User actor,
                                                        @PathVariable String productId) {
        return InventoryRestMapper.toResponses(
                sellerPort.consultInventory(actor, DomainReferences.product(productId)));
    }

    /** Registers a Seller-owned warehouse owned by the authenticated seller. */
    @PostMapping("/warehouses")
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse registerWarehouse(@AuthenticationPrincipal(expression = "user") User actor,
                                               @Valid @RequestBody RegisterWarehouseRequest request) {
        return WarehouseRestMapper.toResponse(sellerPort.registerWarehouse(actor,
                WarehouseRestMapper.newWarehouse(request, WarehouseOwnerType.SELLER)));
    }

    @PostMapping("/inventory/inbound")
    public InventoryItemResponse registerInventoryInbound(@AuthenticationPrincipal(expression = "user") User actor,
                                                          @Valid @RequestBody InventoryMovementRequest request) {
        return InventoryRestMapper.toResponse(sellerPort.registerInventoryInbound(actor,
                DomainReferences.product(request.productId()), DomainReferences.warehouse(request.warehouseId()),
                request.quantity()));
    }

    @PostMapping("/inventory/adjustments")
    public InventoryItemResponse adjustInventory(@AuthenticationPrincipal(expression = "user") User actor,
                                                 @Valid @RequestBody InventoryAdjustmentRequest request) {
        return InventoryRestMapper.toResponse(sellerPort.adjustInventory(actor,
                DomainReferences.product(request.productId()), DomainReferences.warehouse(request.warehouseId()),
                request.quantityDelta()));
    }
}
