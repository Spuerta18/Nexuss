package nexussMarket.adapters.useCases;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.in.LogisticsOperatorPort;
import nexussMarket.domain.services.AdvanceOrderStatusService;
import nexussMarket.domain.services.ChangeUserPasswordService;
import nexussMarket.domain.services.ConfirmDeliveryService;
import nexussMarket.domain.services.ConsultInventoryService;
import nexussMarket.domain.services.ConsultOrderService;
import nexussMarket.domain.services.CreateShipmentService;
import nexussMarket.domain.services.DispatchShipmentService;
import nexussMarket.domain.services.LogoutService;
import nexussMarket.domain.services.PackShipmentService;
import nexussMarket.domain.services.RegisterInventoryReturnService;
import nexussMarket.domain.services.ReportDamagedInventoryService;
import nexussMarket.domain.valueobjects.OrderStatus;

/** Implements {@link LogisticsOperatorPort} by delegating each operation to its domain service. */
@Service
@RequiredArgsConstructor
public class LogisticsOperatorUseCaseImpl implements LogisticsOperatorPort {

    private final ConsultOrderService consultOrderService;
    private final AdvanceOrderStatusService advanceOrderStatusService;
    private final CreateShipmentService createShipmentService;
    private final PackShipmentService packShipmentService;
    private final DispatchShipmentService dispatchShipmentService;
    private final ConfirmDeliveryService confirmDeliveryService;
    private final ReportDamagedInventoryService reportDamagedInventoryService;
    private final RegisterInventoryReturnService registerInventoryReturnService;
    private final ConsultInventoryService consultInventoryService;
    private final LogoutService logoutService;
    private final ChangeUserPasswordService changeUserPasswordService;

    @Override
    public Order consultOrder(User user, Order order) {
        return consultOrderService.execute(user, order);
    }

    @Override
    public Order advanceOrderStatus(User user, Order order, OrderStatus targetStatus) {
        return advanceOrderStatusService.execute(user, order, targetStatus);
    }

    @Override
    public Shipment createShipment(User user, Shipment shipment) {
        return createShipmentService.execute(user, shipment);
    }

    @Override
    public Shipment packShipment(User user, Shipment shipment) {
        return packShipmentService.execute(user, shipment);
    }

    @Override
    public Shipment dispatchShipment(User user, Shipment shipment) {
        return dispatchShipmentService.execute(user, shipment);
    }

    @Override
    public Shipment confirmDelivery(User user, Shipment shipment) {
        return confirmDeliveryService.execute(user, shipment);
    }

    @Override
    public InventoryItem reportDamagedInventory(User user, Product product, Warehouse warehouse, int quantity) {
        return reportDamagedInventoryService.execute(user, product, warehouse, quantity);
    }

    @Override
    public InventoryItem registerInventoryReturn(User user, Product product, Warehouse warehouse, int quantity) {
        return registerInventoryReturnService.execute(user, product, warehouse, quantity);
    }

    @Override
    public List<InventoryItem> consultInventory(User user, Product product) {
        return consultInventoryService.execute(user, product);
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
