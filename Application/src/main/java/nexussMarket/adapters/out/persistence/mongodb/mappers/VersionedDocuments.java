package nexussMarket.adapters.out.persistence.mongodb.mappers;

/** Reads the optimistic-locking version of a stored document into its domain model. */
final class VersionedDocuments {

    private VersionedDocuments() {
    }

    /**
     * The stored version; {@code 0} for a document saved before versioning existed (the startup migration gives
     * those documents {@code version: 0}, so the next save matches).
     */
    static long versionOf(Long version) {
        return version != null ? version : 0L;
    }
}
