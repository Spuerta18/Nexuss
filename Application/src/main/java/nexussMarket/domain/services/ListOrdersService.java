package nexussMarket.domain.services;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.PageQuery;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * A page of orders, most recent first. Read-only. Administrators, supervisors
 * and logistics operators see every order. A seller sees only the orders that
 * hold their products, and in each order only their own lines (the lines of
 * other sellers are none of their business), so the order total is theirs too.
 */
@Service
public class ListOrdersService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ListOrdersService(OrderRepositoryPort orderRepositoryPort, ProductRepositoryPort productRepositoryPort,
                             ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.productRepositoryPort = productRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public ResultPage<Order> execute(User actor, PageQuery page) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR,
                SystemRole.LOGISTICS_OPERATOR, SystemRole.SELLER);
        if (actor.getRole() != SystemRole.SELLER) {
            return orderRepositoryPort.findPage(page);
        }
        Set<String> ownProducts = productRepositoryPort.findBySellerId(actor.getIdentifier()).stream()
                .map(Product::getIdentifier).collect(Collectors.toSet());
        return orderRepositoryPort.findPageByProductIds(ownProducts, page)
                .map(order -> withOnlyLinesOf(order, ownProducts));
    }

    private static Order withOnlyLinesOf(Order order, Set<String> productIds) {
        List<OrderLine> lines = order.getLines().stream()
                .filter(line -> productIds.contains(line.getProduct().getIdentifier()))
                .toList();
        return new Order(order.getIdentifier(), order.getBuyer(), order.getStatus(), lines, order.getCreatedAt());
    }
}
