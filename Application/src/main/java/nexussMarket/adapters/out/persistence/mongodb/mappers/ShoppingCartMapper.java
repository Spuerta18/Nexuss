package nexussMarket.adapters.out.persistence.mongodb.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import nexussMarket.adapters.out.persistence.mongodb.documents.ShoppingCartDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ShoppingCartDocument.CartLineDocument;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.CartLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;

/**
 * Converts {@link ShoppingCart} to and from {@link ShoppingCartDocument}.
 * Cart lines are embedded; the buyer and each line's product are stored as
 * IDs and must be resolved by the caller.
 */
public final class ShoppingCartMapper {

    private ShoppingCartMapper() {
    }

    public static ShoppingCartDocument toDocument(ShoppingCart cart) {
        ShoppingCartDocument document = new ShoppingCartDocument();
        document.setId(cart.getIdentifier());
        document.setBuyerId(cart.getBuyer().getIdentifier());
        document.setLines(cart.getLines().stream().map(ShoppingCartMapper::toDocument).toList());
        return document;
    }

    /**
     * {@code buyer} is the buyer referenced by {@code buyerId}; {@code productsById}
     * holds, by identifier, every product referenced by the cart lines.
     */
    public static ShoppingCart toDomain(ShoppingCartDocument document, Buyer buyer, Map<String, Product> productsById) {
        ShoppingCart cart = new ShoppingCart(document.getId(), buyer);
        cart.setLines(document.getLines().stream()
                .map(line -> new CartLine(productsById.get(line.getProductId()), line.getQuantity()))
                .collect(Collectors.toCollection(ArrayList::new)));
        return cart;
    }

    /** Identifiers of every product referenced by the cart lines. */
    public static List<String> productIds(ShoppingCartDocument document) {
        return document.getLines().stream().map(CartLineDocument::getProductId).distinct().toList();
    }

    private static CartLineDocument toDocument(CartLine line) {
        CartLineDocument document = new CartLineDocument();
        document.setProductId(line.getProduct().getIdentifier());
        document.setQuantity(line.getQuantity());
        return document;
    }
}
