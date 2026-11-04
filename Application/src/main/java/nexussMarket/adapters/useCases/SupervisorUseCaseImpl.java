package nexussMarket.adapters.useCases;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Invoice;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.in.SupervisorPort;
import nexussMarket.domain.services.ChangeUserPasswordService;
import nexussMarket.domain.services.ConsultAuditLogsService;
import nexussMarket.domain.services.ConsultInventoryService;
import nexussMarket.domain.services.ConsultOperationsService;
import nexussMarket.domain.services.ConsultOrderInvoiceService;
import nexussMarket.domain.services.ConsultOrderService;
import nexussMarket.domain.services.ConsultProductService;
import nexussMarket.domain.services.ConsultUserService;
import nexussMarket.domain.services.ListInventoryService;
import nexussMarket.domain.services.ListOrdersService;
import nexussMarket.domain.services.ListPublishedProductsService;
import nexussMarket.domain.services.ListUsersService;
import nexussMarket.domain.services.ListWarehousesService;
import nexussMarket.domain.services.LogoutService;
import nexussMarket.domain.services.UpdateProfileService;
import nexussMarket.domain.valueobjects.PageQuery;
import nexussMarket.domain.valueobjects.SystemRole;

/** Implements {@link SupervisorPort} by delegating each operation to its domain service. */
@Service
@RequiredArgsConstructor
public class SupervisorUseCaseImpl implements SupervisorPort {

    private final ConsultOrderService consultOrderService;
    private final ConsultInventoryService consultInventoryService;
    private final ConsultProductService consultProductService;
    private final ConsultUserService consultUserService;
    private final ConsultOperationsService consultOperationsService;
    private final ConsultAuditLogsService consultAuditLogsService;
    private final ListPublishedProductsService listPublishedProductsService;
    private final ConsultOrderInvoiceService consultOrderInvoiceService;
    private final LogoutService logoutService;
    private final ListUsersService listUsersService;
    private final ListOrdersService listOrdersService;
    private final ListWarehousesService listWarehousesService;
    private final ListInventoryService listInventoryService;
    private final UpdateProfileService updateProfileService;
    private final ChangeUserPasswordService changeUserPasswordService;

    @Override
    public Order consultOrder(User user, Order order) {
        return consultOrderService.execute(user, order);
    }

    @Override
    public List<InventoryItem> consultInventory(User user, Product product) {
        return consultInventoryService.execute(user, product);
    }

    @Override
    public Product consultProduct(User user, Product product) {
        return consultProductService.execute(user, product);
    }

    @Override
    public User consultUser(User user, User target) {
        return consultUserService.execute(user, target);
    }

    @Override
    public List<Product> listPublishedProducts(User user) {
        return listPublishedProductsService.execute(user);
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
    public Invoice consultOrderInvoice(User user, Order order) {
        return consultOrderInvoiceService.execute(user, order);
    }

    @Override
    public void logout(User user) {
        logoutService.execute(user);
    }

    @Override
    public void changePassword(User user, String currentPassword, String newPassword) {
        changeUserPasswordService.execute(user, currentPassword, newPassword);
    }

    @Override
    public User updateProfile(User user, User changes) {
        return updateProfileService.execute(user, changes);
    }

    @Override
    public ResultPage<User> listUsers(User user, SystemRole role, PageQuery page) {
        return listUsersService.execute(user, role, page);
    }

    @Override
    public ResultPage<Order> listOrders(User user, PageQuery page) {
        return listOrdersService.execute(user, page);
    }

    @Override
    public ResultPage<Warehouse> listWarehouses(User user, PageQuery page) {
        return listWarehousesService.execute(user, page);
    }

    @Override
    public ResultPage<InventoryItem> listInventory(User user, PageQuery page) {
        return listInventoryService.execute(user, page);
    }
}
