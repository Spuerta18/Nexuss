package nexussMarket.adapters.in.rest.mappers;

import java.util.List;

import nexussMarket.adapters.in.rest.requests.ConfirmOrderRequest;
import nexussMarket.adapters.in.rest.responses.OrderLineResponse;
import nexussMarket.adapters.in.rest.responses.OrderResponse;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.valueobjects.OrderStatus;

/** Converts order requests into domain models and orders into {@link OrderResponse}. */
public final class OrderRestMapper {

    private OrderRestMapper() {
    }

    /** Carries only the identifier of the order to create; the service builds the order from the cart. */
    public static Order newOrder(ConfirmOrderRequest request) {
        return DomainReferences.order(request.orderId());
    }

    public static OrderStatus orderStatus(String code) {
        return RestCodes.resolve(OrderStatus::fromCode, code, "order status");
    }

    public static OrderResponse toResponse(Order order) {
        return new OrderResponse(order.getIdentifier(),
                order.getBuyer() != null ? order.getBuyer().getIdentifier() : null,
                RestCodes.code(order.getStatus()), order.getCreatedAt(),
                order.getLines().stream().map(OrderRestMapper::toResponse).toList());
    }

    public static List<OrderResponse> toResponses(List<Order> orders) {
        return orders.stream().map(OrderRestMapper::toResponse).toList();
    }

    private static OrderLineResponse toResponse(OrderLine line) {
        return new OrderLineResponse(line.getProduct() != null ? line.getProduct().getIdentifier() : null,
                line.getProductName(), line.getQuantity(), CatalogRestMapper.toResponse(line.getUnitPrice()),
                line.getWarehouse() != null ? line.getWarehouse().getIdentifier() : null);
    }
}
