package nexussMarket.adapters.useCases;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.in.AdministratorPort;
import nexussMarket.domain.services.ApproveReturnService;
import nexussMarket.domain.services.ChangeUserPasswordService;
import nexussMarket.domain.services.ConsultAuditLogsService;
import nexussMarket.domain.services.ConsultOperationsService;
import nexussMarket.domain.services.ConsultUserService;
import nexussMarket.domain.services.DeactivateWarehouseService;
import nexussMarket.domain.services.LogoutService;
import nexussMarket.domain.services.ProcessRefundService;
import nexussMarket.domain.services.RegisterLogisticsOperatorService;
import nexussMarket.domain.services.RegisterSellerService;
import nexussMarket.domain.services.RegisterSupervisorService;
import nexussMarket.domain.services.RegisterWarehouseService;
import nexussMarket.domain.services.RejectReturnService;
import nexussMarket.domain.services.UpdateSellerStatusService;
import nexussMarket.domain.services.UpdateUserStatusService;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.UserStatus;

/** Implements {@link AdministratorPort} by delegating each operation to its domain service. */
@Service
@RequiredArgsConstructor
public class AdministratorUseCaseImpl implements AdministratorPort {

    private final RegisterSellerService registerSellerService;
    private final RegisterSupervisorService registerSupervisorService;
    private final RegisterLogisticsOperatorService registerLogisticsOperatorService;
    private final UpdateSellerStatusService updateSellerStatusService;
    private final UpdateUserStatusService updateUserStatusService;
    private final RegisterWarehouseService registerWarehouseService;
    private final DeactivateWarehouseService deactivateWarehouseService;
    private final ApproveReturnService approveReturnService;
    private final RejectReturnService rejectReturnService;
    private final ProcessRefundService processRefundService;
    private final ConsultUserService consultUserService;
    private final ConsultOperationsService consultOperationsService;
    private final ConsultAuditLogsService consultAuditLogsService;
    private final LogoutService logoutService;
    private final ChangeUserPasswordService changeUserPasswordService;

    @Override
    public Seller registerSeller(User user, Seller seller, String password) {
        return registerSellerService.execute(user, seller, password);
    }

    @Override
    public Supervisor registerSupervisor(User user, Supervisor supervisor, String password) {
        return registerSupervisorService.execute(user, supervisor, password);
    }

    @Override
    public LogisticsOperator registerLogisticsOperator(User user, LogisticsOperator logisticsOperator, String password) {
        return registerLogisticsOperatorService.execute(user, logisticsOperator, password);
    }

    @Override
    public Seller updateSellerStatus(User user, Seller seller, SellerStatus status) {
        return updateSellerStatusService.execute(user, seller, status);
    }

    @Override
    public User updateUserStatus(User user, User target, UserStatus status) {
        return updateUserStatusService.execute(user, target, status);
    }

    @Override
    public Warehouse registerMarketplaceWarehouse(User user, Warehouse warehouse) {
        return registerWarehouseService.execute(user, warehouse);
    }

    @Override
    public Warehouse deactivateWarehouse(User user, Warehouse warehouse) {
        return deactivateWarehouseService.execute(user, warehouse);
    }

    @Override
    public ReturnRequest approveReturn(User user, ReturnRequest returnRequest) {
        return approveReturnService.execute(user, returnRequest);
    }

    @Override
    public ReturnRequest rejectReturn(User user, ReturnRequest returnRequest) {
        return rejectReturnService.execute(user, returnRequest);
    }

    @Override
    public Refund processRefund(User user, Refund refund) {
        return processRefundService.execute(user, refund);
    }

    @Override
    public User consultUser(User user, User target) {
        return consultUserService.execute(user, target);
    }

    @Override
    public List<Operation> consultOperations(User user) {
        return consultOperationsService.execute(user);
    }

    @Override
    public List<AuditLog> consultAuditLogs(User user) {
        return consultAuditLogsService.execute(user);
    }

    @Override
    public void logout(User user) {
        logoutService.execute(user);
    }

    @Override
    public void changePassword(User user, String currentPassword, String newPassword) {
        changeUserPasswordService.execute(user, currentPassword, newPassword);
    }
}
