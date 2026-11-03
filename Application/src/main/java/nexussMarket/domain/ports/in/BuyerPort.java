package nexussMarket.domain.ports.in;

import java.util.List;

import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;

/**
 * Operations a {@link Buyer} can execute. The buyer's own profile is read from
 * the authenticated principal, not through a use case.
 */
public interface BuyerPort {

    ShoppingCart createShoppingCart(User user, ShoppingCart cart);

    ShoppingCart addCartLine(User user, ShoppingCart cart, Product product, int quantity);

    ShoppingCart updateCartLineQuantity(User user, ShoppingCart cart, Product product, int quantity);

    ShoppingCart removeCartLine(User user, ShoppingCart cart, Product product);

    ShoppingCart consultCart(User user, ShoppingCart cart);

    /** {@code newOrder} carries the identifier of the order to create, or none to have it generated. */
    Order confirmOrder(User user, ShoppingCart cart, Order newOrder);

    /** The buyer's active cart (a buyer has at most one). */
    ShoppingCart consultActiveCart(User user);

    /** Adds a line to the buyer's active cart, creating that cart if the buyer has none. */
    ShoppingCart addActiveCartLine(User user, Product product, int quantity);

    ShoppingCart updateActiveCartLineQuantity(User user, Product product, int quantity);

    ShoppingCart removeActiveCartLine(User user, Product product);

    /** Confirms the buyer's active cart; {@code newOrder} carries the order identifier, or none to have it generated. */
    Order confirmActiveCart(User user, Order newOrder);

    Order cancelOrder(User user, Order order);

    Order consultOrder(User user, Order order);

    List<Order> listBuyerOrders(User user, Buyer buyer);

    List<Product> listPublishedProducts(User user);

    Product consultProduct(User user, Product product);

    ReturnRequest requestReturn(User user, ReturnRequest returnRequest);

    /** Logs {@code user} out, revoking every access token issued to them. */
    void logout(User user);

    /** Changes {@code user}'s own password and revokes every access token issued to them. */
    void changePassword(User user, String currentPassword, String newPassword);
}
