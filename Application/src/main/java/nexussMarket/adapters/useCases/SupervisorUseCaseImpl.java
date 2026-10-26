package nexussMarket.adapters.useCases;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.SupervisorPort;
import nexussMarket.domain.services.ChangeUserPasswordService;
import nexussMarket.domain.services.ConsultAuditLogsService;
import nexussMarket.domain.services.ConsultInventoryService;
import nexussMarket.domain.services.ConsultOperationsService;
import nexussMarket.domain.services.ConsultOrderService;
import nexussMarket.domain.services.ConsultProductService;
import nexussMarket.domain.services.ConsultUserService;
import nexussMarket.domain.services.ListPublishedProductsService;
import nexussMarket.domain.services.LogoutService;

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
    private final LogoutService logoutService;
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
    public void logout(User user) {
        logoutService.execute(user);
    }

    @Override
    public void changePassword(User user, String currentPassword, String newPassword) {
        changeUserPasswordService.execute(user, currentPassword, newPassword);
    }
}
