package nexussMarket.domain.ports.in;

import java.util.List;

import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductVariant;

/** Operations a {@link Seller} can execute on their own catalog, warehouses and inventory. */
public interface SellerPort {

    Product publishProduct(User user, Product product);

    Product updateProductStatus(User user, Product product, ProductStatus status);

    Product updateProductVariants(User user, Product product, List<ProductVariant> variants);

    List<Product> listSellerProducts(User user, Seller seller);

    Product consultProduct(User user, Product product);

    /** Registers a Seller-owned warehouse, owned by {@code user}. */
    Warehouse registerWarehouse(User user, Warehouse warehouse);

    InventoryItem registerInventoryInbound(User user, Product product, Warehouse warehouse, int quantity);

    InventoryItem adjustInventory(User user, Product product, Warehouse warehouse, int quantityDelta);

    List<InventoryItem> consultInventory(User user, Product product);

    /** Logs {@code user} out, revoking every access token issued to them. */
    void logout(User user);

    /** Changes {@code user}'s own password and revokes every access token issued to them. */
    void changePassword(User user, String currentPassword, String newPassword);
}
