package nexussMarket.domain.exceptions;

/**
 * The entity changed between the moment it was read and the moment it was
 * saved: another request saved it first (optimistic locking). Nothing of the
 * losing change is stored; the client may reload and retry.
 */
public class ConcurrentUpdateException extends DomainException {

    public ConcurrentUpdateException(String message) {
        super(message);
    }
}
