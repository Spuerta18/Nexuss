package nexussMarket.adapters.out.persistence.mongodb.adapters;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import nexussMarket.adapters.out.persistence.mongodb.documents.InventoryItemDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.OrderDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.RefundDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ReturnRequestDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ShipmentDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ShoppingCartDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.UserDocument;
import nexussMarket.domain.valueobjects.EmailAddress;

/**
 * Brings documents stored before optimistic locking up to date, once, at
 * startup; running it again changes nothing.
 *
 * <ul>
 *   <li>Every versioned collection: a document without {@code version} gets
 *       {@code version: 0}, so the next versioned save matches it.</li>
 *   <li>{@code users}: an email that is not in canonical form (blanks around
 *       it, upper-case letters) is normalized, unless another user already
 *       holds the normalized email; that case is logged and left as it is.</li>
 * </ul>
 *
 * <p>Turned off with {@code nexus.mongo.migrations.enabled=false} (the tests
 * run without a MongoDB server).</p>
 */
@Component
@Profile("!memory")
@ConditionalOnProperty(name = "nexus.mongo.migrations.enabled", havingValue = "true", matchIfMissing = true)
public class VersionFieldMigration implements InitializingBean {

    static final List<Class<?>> VERSIONED_DOCUMENTS = List.of(InventoryItemDocument.class, OrderDocument.class,
            ShipmentDocument.class, ReturnRequestDocument.class, RefundDocument.class, ShoppingCartDocument.class,
            UserDocument.class);

    private static final Logger log = LoggerFactory.getLogger(VersionFieldMigration.class);

    private final MongoTemplate mongoTemplate;

    public VersionFieldMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        addMissingVersions();
        normalizeEmails();
    }

    /** Gives {@code version: 0} to every document stored without one; returns how many were changed. */
    long addMissingVersions() {
        long total = 0;
        for (Class<?> type : VERSIONED_DOCUMENTS) {
            long changed = mongoTemplate.updateMulti(query(where("version").exists(false)),
                    new Update().set("version", 0L), type).getModifiedCount();
            if (changed > 0) {
                log.info("Added version 0 to {} {} documents stored before optimistic locking", changed,
                        mongoTemplate.getCollectionName(type));
            }
            total += changed;
        }
        return total;
    }

    /** Normalizes stored emails that are not in canonical form; returns how many were changed. */
    long normalizeEmails() {
        long changed = 0;
        List<UserDocument> candidates = mongoTemplate.find(
                query(where("email").regex("[A-Z]|^\\s|\\s$")), UserDocument.class);
        for (UserDocument user : candidates) {
            String normalized = EmailAddress.normalize(user.getEmail());
            boolean taken = mongoTemplate.exists(
                    query(where("email").is(normalized).and("_id").ne(user.getId())), UserDocument.class);
            if (taken) {
                log.warn("User {} keeps a non-normalized email: another user already has its normalized form",
                        user.getId());
                continue;
            }
            mongoTemplate.updateFirst(query(where("_id").is(user.getId())),
                    new Update().set("email", normalized).inc("version", 1), UserDocument.class);
            changed++;
        }
        if (changed > 0) {
            log.info("Normalized the email of {} users", changed);
        }
        return changed;
    }
}
