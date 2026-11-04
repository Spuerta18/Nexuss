package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.function.Supplier;

import org.springframework.dao.OptimisticLockingFailureException;

import nexussMarket.domain.exceptions.ConcurrentUpdateException;

/**
 * Runs the versioned {@code save} of the repository adapters. Spring Data
 * matches the document's {@code @Version} and increments it; when the stored
 * version differs (someone saved it after it was read) the update matches
 * nothing and fails with {@link OptimisticLockingFailureException}, translated
 * here into the domain's {@link ConcurrentUpdateException} (409).
 */
final class MongoVersioning {

    private MongoVersioning() {
    }

    /** Runs {@code save} and returns the stored document, which carries the new version. */
    static <D> D save(Supplier<D> save, String entity) {
        try {
            return save.get();
        } catch (OptimisticLockingFailureException e) {
            throw conflict(entity);
        }
    }

    /** Runs a versioned delete. */
    static void delete(Runnable delete, String entity) {
        try {
            delete.run();
        } catch (OptimisticLockingFailureException e) {
            throw conflict(entity);
        }
    }

    private static ConcurrentUpdateException conflict(String entity) {
        return new ConcurrentUpdateException(entity + " was changed by another request; reload it and try again");
    }
}
