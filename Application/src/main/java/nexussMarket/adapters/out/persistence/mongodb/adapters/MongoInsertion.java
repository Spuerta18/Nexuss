package nexussMarket.adapters.out.persistence.mongodb.adapters;

import org.springframework.dao.DuplicateKeyException;

import nexussMarket.domain.exceptions.DuplicateResourceException;

/**
 * Runs a real MongoDB {@code insert} for the {@code insert} methods of the
 * repository adapters. The server rejects a document whose {@code _id} (or a
 * unique index) already exists, so the stored document is never replaced and
 * there is no window between a check and the write. The rejection is
 * translated into {@link DuplicateResourceException}.
 */
final class MongoInsertion {

    private MongoInsertion() {
    }

    /** Runs {@code insert}; a duplicate key fails with {@code conflictMessage}. */
    static void insert(Runnable insert, String conflictMessage) {
        try {
            insert.run();
        } catch (DuplicateKeyException e) {
            throw new DuplicateResourceException(conflictMessage);
        }
    }
}
