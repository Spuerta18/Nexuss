package nexussMarket.adapters.out.persistence.mongodb.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import nexussMarket.adapters.out.persistence.mongodb.documents.OrderDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.OrderDocument.OrderLineDocument;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.OrderStatus;

/**
 * Converts {@link Order} to and from {@link OrderDocument}. Order lines are
 * embedded and keep the {@code productName} / {@code unitPrice} snapshot; the
 * buyer, and each line's product and warehouse, are stored as IDs and must be
 * resolved by the caller.
 */
public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderDocument toDocument(Order order) {
        OrderDocument document = new OrderDocument();
        document.setId(order.getIdentifier());
        document.setBuyerId(order.getBuyer().getIdentifier());
        document.setStatus(order.getStatus().getCode());
        document.setCreatedAt(order.getCreatedAt());
        document.setLines(order.getLines().stream().map(OrderMapper::toDocument).toList());
        return document;
    }

    /**
     * {@code buyer} is the buyer referenced by {@code buyerId}; {@code productsById}
     * and {@code warehousesById} hold, by identifier, every product and warehouse
     * referenced by the order lines.
     */
    public static Order toDomain(OrderDocument document, Buyer buyer, Map<String, Product> productsById,
                                 Map<String, Warehouse> warehousesById) {
        Order order = new Order(document.getId(), buyer,
                CatalogCodes.resolve(OrderStatus::fromCode, document.getStatus(), "OrderStatus"));
        order.setCreatedAt(document.getCreatedAt());
        order.setLines(document.getLines().stream()
                .map(line -> new OrderLine(productsById.get(line.getProductId()), line.getProductName(),
                        line.getQuantity(), line.getUnitPrice(), warehousesById.get(line.getWarehouseId())))
                .collect(Collectors.toCollection(ArrayList::new)));
        return order;
    }

    /** Identifiers of every product referenced by the order lines. */
    public static List<String> productIds(OrderDocument document) {
        return document.getLines().stream().map(OrderLineDocument::getProductId).distinct().toList();
    }

    /** Identifiers of every warehouse referenced by the order lines. */
    public static List<String> warehouseIds(OrderDocument document) {
        return document.getLines().stream().map(OrderLineDocument::getWarehouseId).distinct().toList();
    }

    private static OrderLineDocument toDocument(OrderLine line) {
        OrderLineDocument document = new OrderLineDocument();
        document.setProductId(line.getProduct().getIdentifier());
        document.setProductName(line.getProductName());
        document.setQuantity(line.getQuantity());
        document.setUnitPrice(line.getUnitPrice());
        document.setWarehouseId(line.getWarehouse().getIdentifier());
        return document;
    }
}
