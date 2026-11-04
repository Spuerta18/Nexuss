package nexussMarket.domain.ports.in;

import java.util.List;

import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Invoice;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.PageQuery;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.SystemRole;
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

    /** Records the payment of an order: {@code PENDING_PAYMENT} to {@code PAID}, issuing its invoice. */
    Order registerOrderPayment(User user, Order order);

    /** The invoice of an order, issued when it was paid. */
    Invoice consultOrderInvoice(User user, Order order);

    /** Logs {@code user} out, revoking every access token issued to them. */
    void logout(User user);

    /** Changes {@code user}'s own password and revokes every access token issued to them. */
    void changePassword(User user, String currentPassword, String newPassword);

    /**
     * Changes the caller's own profile: {@code changes} carries the new full name (and, for a buyer, the
     * addresses). Email, role and status never change here.
     */
    User updateProfile(User user, User changes);

    /** A page of users, only those of {@code role} when it is not {@code null}. Read-only. */
    ResultPage<User> listUsers(User user, SystemRole role, PageQuery page);

    /** A page of orders, most recent first. Read-only. */
    ResultPage<Order> listOrders(User user, PageQuery page);

    /** A page of warehouses. Read-only. */
    ResultPage<Warehouse> listWarehouses(User user, PageQuery page);

    /** A page of inventory records. Read-only. */
    ResultPage<InventoryItem> listInventory(User user, PageQuery page);
}
