package nexussMarket.adapters.out.persistence.mongodb.adapters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import nexussMarket.adapters.out.persistence.mongodb.documents.ShoppingCartDocument;

/**
 * Makes {@code shopping_carts.buyerId} unique, so a buyer has at most one
 * active cart even when two carts are created at the same time: the losing
 * {@code insert} fails with a duplicate key (409).
 *
 * <p>Created here instead of through {@code @Indexed}, because databases
 * created before this rule already hold a non-unique index with the same name
 * and key, and MongoDB refuses to create the unique one next to it (startup
 * would fail). That legacy index is dropped first. If stored data already has
 * a buyer with several carts the unique index cannot be built: the error is
 * logged, the application still starts, and the rule is then only checked by
 * {@code CreateShoppingCartService}, without the race guarantee.</p>
 *
 * <p>Runs only with {@code spring.data.mongodb.auto-index-creation=true}, like
 * the annotated indexes; the tests turn it off because they run without a
 * MongoDB server.</p>
 */
@Component
@ConditionalOnProperty(name = "spring.data.mongodb.auto-index-creation", havingValue = "true")
public class ActiveCartIndexInitializer implements InitializingBean {

    static final String INDEX_NAME = "buyerId";

    private static final Logger log = LoggerFactory.getLogger(ActiveCartIndexInitializer.class);

    private final MongoTemplate mongoTemplate;

    public ActiveCartIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        IndexOperations indexes = mongoTemplate.indexOps(ShoppingCartDocument.class);
        boolean legacyIndex = indexes.getIndexInfo().stream()
                .anyMatch(index -> INDEX_NAME.equals(index.getName()) && !index.isUnique());
        if (legacyIndex) {
            indexes.dropIndex(INDEX_NAME);
            log.info("Dropped the non-unique shopping_carts index {} to replace it with a unique one", INDEX_NAME);
        }
        try {
            indexes.createIndex(new Index("buyerId", Sort.Direction.ASC).named(INDEX_NAME).unique());
        } catch (DataAccessException e) {
            log.error("Could not make shopping_carts.buyerId unique (a buyer has several carts?); one active cart"
                    + " per buyer is only checked by the service, not enforced by the database", e);
        }
    }
}
