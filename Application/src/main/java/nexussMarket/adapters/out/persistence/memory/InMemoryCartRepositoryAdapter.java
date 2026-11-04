package nexussMarket.adapters.out.persistence.memory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.ShoppingCartDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.ShoppingCartMapper;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.ports.out.CartRepositoryPort;

/** {@link CartRepositoryPort} on the in-memory {@code shopping_carts} collection (unique buyer, versioned). */
@Repository
@Profile("memory")
public class InMemoryCartRepositoryAdapter implements CartRepositoryPort {

    private final InMemoryDatabase database;
    private final InMemoryReferenceResolver referenceResolver;

    public InMemoryCartRepositoryAdapter(InMemoryDatabase database, InMemoryReferenceResolver referenceResolver) {
        this.database = database;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public ShoppingCart insert(ShoppingCart cart) {
        database.carts.insert(ShoppingCartMapper.toDocument(cart), "A shopping cart with id " + cart.getIdentifier()
                + ", or an active cart of buyer " + cart.getBuyer().getIdentifier() + ", already exists");
        return cart;
    }

    @Override
    public ShoppingCart save(ShoppingCart cart) {
        ShoppingCartDocument saved = database.carts.save(ShoppingCartMapper.toDocument(cart),
                "Shopping cart " + cart.getIdentifier());
        cart.restoreVersion(saved.getVersion());
        return cart;
    }

    @Override
    public Optional<ShoppingCart> findById(String identifier) {
        return database.carts.findById(identifier).map(document -> toDomain(List.of(document)).get(0));
    }

    @Override
    public List<ShoppingCart> findByBuyerId(String buyerId) {
        return toDomain(database.carts.find(document -> buyerId.equals(document.getBuyerId())));
    }

    @Override
    public void delete(ShoppingCart cart) {
        database.carts.delete(ShoppingCartMapper.toDocument(cart), "Shopping cart " + cart.getIdentifier());
    }

    private List<ShoppingCart> toDomain(List<ShoppingCartDocument> documents) {
        if (documents.isEmpty()) {
            return List.of();
        }
        Map<String, Buyer> buyers = referenceResolver.users(
                documents.stream().map(ShoppingCartDocument::getBuyerId).toList(), Buyer.class);
        Map<String, Product> products = referenceResolver.products(
                documents.stream().flatMap(document -> ShoppingCartMapper.productIds(document).stream()).toList());
        return documents.stream()
                .map(document -> ShoppingCartMapper.toDomain(document, buyers.get(document.getBuyerId()), products))
                .toList();
    }
}
