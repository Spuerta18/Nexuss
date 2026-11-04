package nexussMarket.domain.ports.in;

import java.util.List;

import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Invoice;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.PageQuery;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Read-only operations a {@link Supervisor} can execute.
 *
 * <p>{@code target} in {@link #consultUser} is typed as the abstract
 * {@link User}: a caller that only knows a user identifier cannot build a
 * reference from it without knowing the concrete role, so the REST adapter
 * asks the client for the role of the target user and builds a reference of
 * that subtype.</p>
 */
public interface SupervisorPort {

    Order consultOrder(User user, Order order);

    List<InventoryItem> consultInventory(User user, Product product);

    Product consultProduct(User user, Product product);

    User consultUser(User user, User target);

    List<Product> listPublishedProducts(User user);

    /** The operation history, most recent first. */
    List<Operation> consultOperations(User user);

    /** The audit trail, most recent first. */
    List<AuditLog> consultAuditLogs(User user);

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
