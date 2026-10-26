package nexussMarket.domain.ports.in;

import java.util.List;

import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.UserStatus;

/**
 * Operations an {@link Administrator} can execute: staff registration, user
 * and seller status, Marketplace-owned warehouses, returns and refunds.
 *
 * <p>{@code target} parameters are typed as the abstract {@link User}: a caller
 * that only knows a user identifier cannot build a reference from it without
 * knowing the concrete role, so the REST adapter asks the client for the role
 * of the target user and builds a reference of that subtype.</p>
 */
public interface AdministratorPort {

    Seller registerSeller(User user, Seller seller, String password);

    Supervisor registerSupervisor(User user, Supervisor supervisor, String password);

    LogisticsOperator registerLogisticsOperator(User user, LogisticsOperator logisticsOperator, String password);

    Seller updateSellerStatus(User user, Seller seller, SellerStatus status);

    User updateUserStatus(User user, User target, UserStatus status);

    /** Registers a Marketplace-owned warehouse. */
    Warehouse registerMarketplaceWarehouse(User user, Warehouse warehouse);

    Warehouse deactivateWarehouse(User user, Warehouse warehouse);

    ReturnRequest approveReturn(User user, ReturnRequest returnRequest);

    ReturnRequest rejectReturn(User user, ReturnRequest returnRequest);

    Refund processRefund(User user, Refund refund);

    User consultUser(User user, User target);

    /** The operation history, most recent first. */
    List<Operation> consultOperations(User user);

    /** The audit trail, most recent first. */
    List<AuditLog> consultAuditLogs(User user);
}
