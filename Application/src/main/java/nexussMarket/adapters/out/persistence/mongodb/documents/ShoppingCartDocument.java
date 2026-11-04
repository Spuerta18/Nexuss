package nexussMarket.adapters.out.persistence.mongodb.documents;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A buyer's {@code ShoppingCart}. Cart lines are embedded; the buyer and each
 * line's product are referenced by ID. {@code buyerId} is unique (a buyer has
 * at most one active cart); that index is created by
 * {@code ActiveCartIndexInitializer}, not by an annotation.
 */
@Document(collection = "shopping_carts")
@Getter
@Setter
@NoArgsConstructor
public class ShoppingCartDocument {

    @Id
    private String id;

    private String buyerId;

    private List<CartLineDocument> lines = new ArrayList<>();

    /**
     * Optimistic-locking version ({@code @Version}): a save matches only the version it read and increments it.
     * Documents stored before versioning get {@code 0} from {@code VersionFieldMigration} at startup.
     */
    @Version
    private Long version;

    /** Embedded {@code CartLine}. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CartLineDocument {

        private String productId;
        private int quantity;
    }
}
