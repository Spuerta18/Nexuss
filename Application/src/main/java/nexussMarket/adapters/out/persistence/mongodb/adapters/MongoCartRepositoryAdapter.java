package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.ShoppingCartDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.ShoppingCartMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.ShoppingCartMongoRepository;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.ports.out.CartRepositoryPort;

/** Implements {@link CartRepositoryPort} on the {@code shopping_carts} collection. */
@Repository
@Profile("!memory")
public class MongoCartRepositoryAdapter implements CartRepositoryPort {

    private final ShoppingCartMongoRepository shoppingCartRepository;
    private final MongoReferenceResolver referenceResolver;

    public MongoCartRepositoryAdapter(ShoppingCartMongoRepository shoppingCartRepository,
                                      MongoReferenceResolver referenceResolver) {
        this.shoppingCartRepository = shoppingCartRepository;
        this.referenceResolver = referenceResolver;
    }

    /** A real {@code insert}: an existing document with the same key is never replaced. */
    @Override
    public ShoppingCart insert(ShoppingCart cart) {
        MongoInsertion.insert(() -> shoppingCartRepository.insert(ShoppingCartMapper.toDocument(cart)),
                "A shopping cart with id " + cart.getIdentifier()
                + ", or an active cart of buyer " + cart.getBuyer().getIdentifier() + ", already exists");
        return cart;
    }

    @Override
    public ShoppingCart save(ShoppingCart cart) {
        cart.restoreVersion(MongoVersioning.save(() -> shoppingCartRepository.save(ShoppingCartMapper.toDocument(cart)),
                "Shopping cart " + cart.getIdentifier()).getVersion());
        return cart;
    }

    @Override
    public Optional<ShoppingCart> findById(String identifier) {
        return shoppingCartRepository.findById(identifier).map(document -> toDomain(List.of(document)).get(0));
    }

    @Override
    public List<ShoppingCart> findByBuyerId(String buyerId) {
        return toDomain(shoppingCartRepository.findAllByBuyerId(buyerId));
    }

    @Override
    public void delete(ShoppingCart cart) {
        // Spring Data deletes a versioned document only at its version, and fails when nothing matched.
        MongoVersioning.delete(() -> shoppingCartRepository.delete(ShoppingCartMapper.toDocument(cart)),
                "Shopping cart " + cart.getIdentifier());
    }

    /** Builds {@code documents} into carts, resolving every reference with a single query per collection. */
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
