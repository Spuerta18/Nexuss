package nexussMarket.adapters.out.persistence.mongodb.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import nexussMarket.adapters.out.persistence.mongodb.documents.ShoppingCartDocument;

/** Spring Data repository for the {@code shopping_carts} collection. */
public interface ShoppingCartMongoRepository extends MongoRepository<ShoppingCartDocument, String> {

    List<ShoppingCartDocument> findAllByBuyerId(String buyerId);
}
