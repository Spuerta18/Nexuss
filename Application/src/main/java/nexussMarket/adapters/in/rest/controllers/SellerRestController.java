package nexussMarket.adapters.in.rest.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nexussMarket.adapters.in.rest.mappers.CatalogRestMapper;
import nexussMarket.adapters.in.rest.mappers.DomainReferences;
import nexussMarket.adapters.in.rest.mappers.InventoryRestMapper;
import nexussMarket.adapters.in.rest.mappers.OrderRestMapper;
import nexussMarket.adapters.in.rest.mappers.PageRestMapper;
import nexussMarket.adapters.in.rest.mappers.ReportRestMapper;
import nexussMarket.adapters.in.rest.mappers.WarehouseRestMapper;
import nexussMarket.adapters.in.rest.requests.ChangePasswordRequest;
import nexussMarket.adapters.in.rest.requests.InventoryAdjustmentRequest;
import nexussMarket.adapters.in.rest.requests.InventoryMovementRequest;
import nexussMarket.adapters.in.rest.requests.ProductStatusRequest;
import nexussMarket.adapters.in.rest.requests.PublishProductRequest;
import nexussMarket.adapters.in.rest.requests.RegisterWarehouseRequest;
import nexussMarket.adapters.in.rest.requests.UpdateProductRequest;
import nexussMarket.adapters.in.rest.requests.VariantsRequest;
import nexussMarket.adapters.in.rest.responses.InventoryItemResponse;
import nexussMarket.adapters.in.rest.responses.OrderResponse;
import nexussMarket.adapters.in.rest.responses.PageResponse;
import nexussMarket.adapters.in.rest.responses.ProductResponse;
import nexussMarket.adapters.in.rest.responses.ReportResponses.SellerPerformanceReportResponse;
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
    /** Changes the name and the price of one of the caller's products. */
    @PutMapping("/products/{productId}")
    public ProductResponse updateProduct(@AuthenticationPrincipal(expression = "user") User actor,
                                         @PathVariable String productId,
                                         @Valid @RequestBody UpdateProductRequest request) {
        return CatalogRestMapper.toResponse(sellerPort.updateProduct(actor,
                CatalogRestMapper.productChanges(productId, request)));
    }

    /**
     * Deletes one of the caller's products logically: it becomes {@code DISCONTINUED} and leaves the catalog.
     * {@code 409} while an open order holds it; calling it again answers the same product.
     */
    @DeleteMapping("/products/{productId}")
    public ProductResponse discontinueProduct(@AuthenticationPrincipal(expression = "user") User actor,
                                              @PathVariable String productId) {
        return CatalogRestMapper.toResponse(sellerPort.discontinueProduct(actor, DomainReferences.product(productId)));
    }

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

    /** Revokes every token of the caller, including the one used for this request. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal(expression = "user") User actor) {
        sellerPort.logout(actor);
    }

    /** Changes the caller's password; every token issued before, including this one, stops working. */
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal(expression = "user") User actor,
                               @Valid @RequestBody ChangePasswordRequest request) {
        sellerPort.changePassword(actor, request.currentPassword(), request.newPassword());
    }

    /** A page of orders, most recent first ({@code page} from 0, {@code size} 1-100, default 20). */
    @GetMapping("/orders")
    public PageResponse<OrderResponse> listOrders(@AuthenticationPrincipal(expression = "user") User actor,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return PageRestMapper.toResponse(sellerPort.listOrders(actor, PageRestMapper.query(page, size)),
                OrderRestMapper::toResponse);
    }

    /** A page of warehouses ({@code page} from 0, {@code size} 1-100, default 20). */
    @GetMapping("/warehouses")
    public PageResponse<WarehouseResponse> listWarehouses(@AuthenticationPrincipal(expression = "user") User actor,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return PageRestMapper.toResponse(sellerPort.listWarehouses(actor, PageRestMapper.query(page, size)),
                WarehouseRestMapper::toResponse);
    }

    /** A page of inventory records ({@code page} from 0, {@code size} 1-100, default 20). */
    @GetMapping("/inventory")
    public PageResponse<InventoryItemResponse> listInventory(@AuthenticationPrincipal(expression = "user") User actor,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return PageRestMapper.toResponse(sellerPort.listInventory(actor, PageRestMapper.query(page, size)),
                InventoryRestMapper::toResponse);
    }

    /** The caller's own results between {@code from} and {@code to} (ISO dates; default the last 30 days). */
    @GetMapping("/reports/performance")
    public SellerPerformanceReportResponse performanceReport(@AuthenticationPrincipal(expression = "user") User actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ReportRestMapper.toResponse(sellerPort.performanceReport(actor, ReportRestMapper.period(from, to)));
    }
}
