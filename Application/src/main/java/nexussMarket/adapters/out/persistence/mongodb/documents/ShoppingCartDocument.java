package nexussMarket.adapters.out.persistence.mongodb.documents;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A buyer's {@code ShoppingCart}. Cart lines are embedded; the buyer and each
 * line's product are referenced by ID.
 */
@Document(collection = "shopping_carts")
@Getter
@Setter
@NoArgsConstructor
public class ShoppingCartDocument {

    @Id
    private String id;

    @Indexed
    private String buyerId;

    private List<CartLineDocument> lines = new ArrayList<>();

    /** Embedded {@code CartLine}. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CartLineDocument {

        private String productId;
        private int quantity;
    }
}
