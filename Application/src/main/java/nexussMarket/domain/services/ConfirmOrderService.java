package nexussMarket.domain.services;

import java.util.ArrayList;
import java.util.List;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.CartLine;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.ports.in.ConfirmOrderUseCase;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.valueobjects.OrderStatus;

public class ConfirmOrderService implements ConfirmOrderUseCase {

    private final CartRepositoryPort cartRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ReserveInventoryService reserveInventoryService;

    public ConfirmOrderService(CartRepositoryPort cartRepositoryPort, OrderRepositoryPort orderRepositoryPort,
                                ReserveInventoryService reserveInventoryService) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.reserveInventoryService = reserveInventoryService;
    }

    @Override
    public Order execute(Command command) {
        ShoppingCart cart = cartRepositoryPort.findById(command.cartId())
                .orElseThrow(() -> new EntityNotFoundException("No shopping cart found with id " + command.cartId()));

        Order order = new Order(command.orderId(), cart.getBuyer(), OrderStatus.PENDING_PAYMENT);

        List<OrderLine> orderLines = new ArrayList<>();
        for (CartLine cartLine : cart.getLines()) {
            Product product = cartLine.getProduct();
            InventoryItem reservedItem = reserveInventoryService.reserve(product.getIdentifier(), cartLine.getQuantity());
            orderLines.add(new OrderLine(product, cartLine.getQuantity(), product.getPrice(),
                    reservedItem.getWarehouse().getIdentifier()));
        }
        order.setLines(orderLines);

        return orderRepositoryPort.save(order);
    }
}
