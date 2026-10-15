package nexussMarket.domain.ports.in;

import java.util.List;

import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;

/**
 * Read-only operations a {@link Supervisor} can execute.
 *
 * <p>{@code target} in {@link #consultUser} is typed as the abstract
 * {@link User}: a caller that only knows a user identifier cannot build a
 * reference from it without knowing the concrete role, so REST adapters will
 * need a way to build user references once they exist.</p>
 */
public interface SupervisorPort {

    Order consultOrder(User user, Order order);

    List<InventoryItem> consultInventory(User user, Product product);

    Product consultProduct(User user, Product product);

    User consultUser(User user, User target);

    List<Product> listPublishedProducts(User user);
}
