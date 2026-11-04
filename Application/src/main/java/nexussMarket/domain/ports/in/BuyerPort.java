package nexussMarket.domain.ports.in;

import java.util.List;

import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Invoice;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.Shipment;
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

    /** Confirms the buyer's active cart; {@code newOrder} carries the order identifier, or none to generate it. */
    Order confirmActiveCart(User user, Order newOrder);

    Order cancelOrder(User user, Order order);

    Order consultOrder(User user, Order order);

    List<Order> listBuyerOrders(User user, Buyer buyer);

    List<Product> listPublishedProducts(User user);

    Product consultProduct(User user, Product product);

    ReturnRequest requestReturn(User user, ReturnRequest returnRequest);

    /** The invoice of an order, issued when it was paid. */
    Invoice consultOrderInvoice(User user, Order order);

    /** The shipment of one of the buyer's orders. */
    Shipment consultOrderShipment(User user, Order order);

    /** Removes every line of the buyer's active cart. */
    ShoppingCart clearActiveCart(User user);

    /** Pays one of the buyer's {@code PENDING_PAYMENT} orders through the payment gateway. */
    Order payOrder(User user, Order order, String paymentToken);

    /** Logs {@code user} out, revoking every access token issued to them. */
    void logout(User user);

    /** Changes {@code user}'s own password and revokes every access token issued to them. */
    void changePassword(User user, String currentPassword, String newPassword);

    /**
     * Changes the caller's own profile: {@code changes} carries the new full name (and, for a buyer, the
     * addresses). Email, role and status never change here.
     */
    User updateProfile(User user, User changes);
}
