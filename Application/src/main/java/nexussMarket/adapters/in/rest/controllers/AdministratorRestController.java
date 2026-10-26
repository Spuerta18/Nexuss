package nexussMarket.adapters.in.rest.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import nexussMarket.adapters.in.rest.mappers.DomainReferences;
import nexussMarket.adapters.in.rest.mappers.ReturnRestMapper;
import nexussMarket.adapters.in.rest.mappers.UserRestMapper;
import nexussMarket.adapters.in.rest.mappers.WarehouseRestMapper;
import nexussMarket.adapters.in.rest.requests.ChangePasswordRequest;
import nexussMarket.adapters.in.rest.requests.RegisterStaffRequest;
import nexussMarket.adapters.in.rest.requests.RegisterWarehouseRequest;
import nexussMarket.adapters.in.rest.requests.SellerStatusRequest;
import nexussMarket.adapters.in.rest.requests.UserStatusRequest;
import nexussMarket.adapters.in.rest.responses.AuditLogResponse;
import nexussMarket.adapters.in.rest.responses.OperationResponse;
import nexussMarket.adapters.in.rest.responses.RefundResponse;
import nexussMarket.adapters.in.rest.responses.ReturnRequestResponse;
import nexussMarket.adapters.in.rest.responses.UserResponse;
import nexussMarket.adapters.in.rest.responses.WarehouseResponse;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.AdministratorPort;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/**
 * Endpoints of the {@code ADMINISTRATOR} role ({@link AdministratorPort}):
 * staff registration, user and seller status, Marketplace-owned warehouses,
 * returns and refunds.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMINISTRATOR')")
@RequiredArgsConstructor
public class AdministratorRestController {

    private final AdministratorPort administratorPort;

    @PostMapping("/sellers")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerSeller(@AuthenticationPrincipal(expression = "user") User actor,
                                       @Valid @RequestBody RegisterStaffRequest request) {
        return UserRestMapper.toResponse(
                administratorPort.registerSeller(actor, UserRestMapper.newSeller(request), request.password()));
    }

    @PostMapping("/supervisors")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerSupervisor(@AuthenticationPrincipal(expression = "user") User actor,
                                           @Valid @RequestBody RegisterStaffRequest request) {
        return UserRestMapper.toResponse(administratorPort.registerSupervisor(actor,
                UserRestMapper.newSupervisor(request), request.password()));
    }

    @PostMapping("/logistics-operators")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerLogisticsOperator(@AuthenticationPrincipal(expression = "user") User actor,
                                                  @Valid @RequestBody RegisterStaffRequest request) {
        return UserRestMapper.toResponse(administratorPort.registerLogisticsOperator(actor,
                UserRestMapper.newLogisticsOperator(request), request.password()));
    }

    @PatchMapping("/sellers/{sellerId}/status")
    public UserResponse updateSellerStatus(@AuthenticationPrincipal(expression = "user") User actor,
                                           @PathVariable String sellerId,
                                           @Valid @RequestBody SellerStatusRequest request) {
        return UserRestMapper.toResponse(administratorPort.updateSellerStatus(actor, DomainReferences.seller(sellerId),
                UserRestMapper.sellerStatus(request.status())));
    }

    /** {@code role} in the body is the role of the user being changed (see {@link UserStatusRequest}). */
    @PatchMapping("/users/{userId}/status")
    public UserResponse updateUserStatus(@AuthenticationPrincipal(expression = "user") User actor,
                                         @PathVariable String userId, @Valid @RequestBody UserStatusRequest request) {
        return UserRestMapper.toResponse(administratorPort.updateUserStatus(actor,
                DomainReferences.user(userId, UserRestMapper.role(request.role())),
                UserRestMapper.userStatus(request.status())));
    }

    /** {@code role} is the role of the user consulted: needed to build a reference of the right subtype. */
    @GetMapping("/users/{userId}")
    public UserResponse consultUser(@AuthenticationPrincipal(expression = "user") User actor,
                                    @PathVariable String userId, @RequestParam String role) {
        return UserRestMapper.toResponse(
                administratorPort.consultUser(actor, DomainReferences.user(userId, UserRestMapper.role(role))));
    }

    /** Registers a Marketplace-owned warehouse. */
    @PostMapping("/warehouses")
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse registerWarehouse(@AuthenticationPrincipal(expression = "user") User actor,
                                               @Valid @RequestBody RegisterWarehouseRequest request) {
        return WarehouseRestMapper.toResponse(administratorPort.registerMarketplaceWarehouse(actor,
                WarehouseRestMapper.newWarehouse(request, WarehouseOwnerType.MARKETPLACE)));
    }

    @PostMapping("/warehouses/{warehouseId}/deactivate")
    public WarehouseResponse deactivateWarehouse(@AuthenticationPrincipal(expression = "user") User actor,
                                                 @PathVariable String warehouseId) {
        return WarehouseRestMapper.toResponse(
                administratorPort.deactivateWarehouse(actor, DomainReferences.warehouse(warehouseId)));
    }

    @PostMapping("/returns/{returnRequestId}/approve")
    public ReturnRequestResponse approveReturn(@AuthenticationPrincipal(expression = "user") User actor,
                                               @PathVariable String returnRequestId) {
        return ReturnRestMapper.toResponse(
                administratorPort.approveReturn(actor, DomainReferences.returnRequest(returnRequestId)));
    }

    @PostMapping("/returns/{returnRequestId}/reject")
    public ReturnRequestResponse rejectReturn(@AuthenticationPrincipal(expression = "user") User actor,
                                              @PathVariable String returnRequestId) {
        return ReturnRestMapper.toResponse(
                administratorPort.rejectReturn(actor, DomainReferences.returnRequest(returnRequestId)));
    }

    @PostMapping("/refunds/{refundId}/process")
    public RefundResponse processRefund(@AuthenticationPrincipal(expression = "user") User actor,
                                        @PathVariable String refundId) {
        return ReturnRestMapper.toResponse(administratorPort.processRefund(actor, DomainReferences.refund(refundId)));
    }

    @GetMapping("/operations")
    public List<OperationResponse> consultOperations(@AuthenticationPrincipal(expression = "user") User actor) {
        return AuditRestMapper.toOperationResponses(administratorPort.consultOperations(actor));
    }

    @GetMapping("/audit-logs")
    public List<AuditLogResponse> consultAuditLogs(@AuthenticationPrincipal(expression = "user") User actor) {
        return AuditRestMapper.toAuditLogResponses(administratorPort.consultAuditLogs(actor));
    }

    /** Revokes every token of the caller, including the one used for this request. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal(expression = "user") User actor) {
        administratorPort.logout(actor);
    }

    /** Changes the caller's password; every token issued before, including this one, stops working. */
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal(expression = "user") User actor,
                               @Valid @RequestBody ChangePasswordRequest request) {
        administratorPort.changePassword(actor, request.currentPassword(), request.newPassword());
    }
}
