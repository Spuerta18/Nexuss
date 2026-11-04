package nexussMarket.adapters.out.persistence.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import nexussMarket.domain.exceptions.ConcurrentUpdateException;
import nexussMarket.domain.exceptions.DuplicateResourceException;

/**
 * One collection of the in-memory store, with the guarantees the MongoDB
 * adapters get from the server: {@link #insert} never replaces a stored
 * document (same id or same unique key, {@link DuplicateResourceException}),
 * and a versioned collection only saves or deletes a document at the version
 * it was read with ({@link ConcurrentUpdateException}), incrementing it on
 * every save. Every method is atomic ({@code synchronized}).
 *
 * <p>Stored documents are never changed in place: a save stores the new
 * document instead, so a document handed to a reader stays consistent.</p>
 *
 * @param <D> the document type, shared with the MongoDB adapters
 */
public final class InMemoryCollection<D> {

    private final String name;
    private final Function<D, String> idOf;
    private final List<Function<D, Object>> uniqueKeys;
    private final Function<D, Long> versionOf;
    private final BiConsumer<D, Long> versionSetter;
    private final Map<String, D> documents = new LinkedHashMap<>();

    private InMemoryCollection(String name, Function<D, String> idOf, List<Function<D, Object>> uniqueKeys,
                               Function<D, Long> versionOf, BiConsumer<D, Long> versionSetter) {
        this.name = name;
        this.idOf = idOf;
        this.uniqueKeys = List.copyOf(uniqueKeys);
        this.versionOf = versionOf;
        this.versionSetter = versionSetter;
    }

    /** A collection without optimistic locking: {@link #save} replaces (or creates) the document. */
    public static <D> InMemoryCollection<D> plain(String name, Function<D, String> idOf,
                                                  List<Function<D, Object>> uniqueKeys) {
        return new InMemoryCollection<>(name, idOf, uniqueKeys, null, null);
    }

    /** A collection with optimistic locking on the document's {@code version}. */
    public static <D> InMemoryCollection<D> versioned(String name, Function<D, String> idOf,
                                                      List<Function<D, Object>> uniqueKeys,
                                                      Function<D, Long> versionOf, BiConsumer<D, Long> versionSetter) {
        return new InMemoryCollection<>(name, idOf, uniqueKeys, Objects.requireNonNull(versionOf),
                Objects.requireNonNull(versionSetter));
    }

    /** Stores a new document; fails if its id or one of its unique keys is already taken. */
    public synchronized D insert(D document, String conflictMessage) {
        String id = idOf.apply(document);
        if (documents.containsKey(id) || takesUniqueKey(document, null)) {
            throw new DuplicateResourceException(conflictMessage);
        }
        if (versionOf != null && versionOf.apply(document) == null) {
            versionSetter.accept(document, 0L);
        }
        documents.put(id, document);
        return document;
    }

    /**
     * Stores the changes of a document. In a versioned collection the stored
     * document must exist at the same version, which is then incremented on
     * {@code document}; otherwise nothing is stored.
     */
    public synchronized D save(D document, String entity) {
        String id = idOf.apply(document);
        D stored = documents.get(id);
        if (versionOf != null) {
            if (stored == null || !Objects.equals(versionOf.apply(stored), versionOf.apply(document))) {
                throw conflict(entity);
            }
            versionSetter.accept(document, versionOf.apply(stored) + 1);
        }
        if (takesUniqueKey(document, id)) {
            throw new DuplicateResourceException(entity + " has a unique value another " + name + " document holds");
        }
        documents.put(id, document);
        return document;
    }

    /** Deletes a document; in a versioned collection only at the version it was read with. */
    public synchronized void delete(D document, String entity) {
        String id = idOf.apply(document);
        D stored = documents.get(id);
        if (versionOf != null
                && (stored == null || !Objects.equals(versionOf.apply(stored), versionOf.apply(document)))) {
            throw conflict(entity);
        }
        documents.remove(id);
    }

    public synchronized Optional<D> findById(String id) {
        return Optional.ofNullable(documents.get(id));
    }

    /** The stored documents with the given ids, in the order of {@code ids}; missing ones are left out. */
    public synchronized List<D> findAllById(Collection<String> ids) {
        List<D> found = new ArrayList<>();
        for (String id : ids) {
            D document = documents.get(id);
            if (document != null) {
                found.add(document);
            }
        }
        return found;
    }

    /** The stored documents matching {@code filter}, in insertion order. */
    public synchronized List<D> find(Predicate<D> filter) {
        return documents.values().stream().filter(filter).toList();
    }

    public synchronized List<D> findAll() {
        return List.copyOf(documents.values());
    }

    public synchronized boolean exists(Predicate<D> filter) {
        return documents.values().stream().anyMatch(filter);
    }

    public synchronized int size() {
        return documents.size();
    }

    private boolean takesUniqueKey(D document, String ownId) {
        for (Function<D, Object> key : uniqueKeys) {
            Object value = key.apply(document);
            if (value == null) {
                continue;
            }
            for (Map.Entry<String, D> entry : documents.entrySet()) {
                if (!entry.getKey().equals(ownId) && value.equals(key.apply(entry.getValue()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ConcurrentUpdateException conflict(String entity) {
        return new ConcurrentUpdateException(entity + " was changed by another request; reload it and try again");
    }
}
