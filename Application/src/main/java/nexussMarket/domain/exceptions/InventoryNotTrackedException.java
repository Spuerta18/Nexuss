package nexussMarket.domain.exceptions;

/**
 * Stock was registered for a {@code DIGITAL} product, which neither requires nor allows inventory.
 */
public class InventoryNotTrackedException extends DomainException {

    public InventoryNotTrackedException(String message) {
        super(message);
    }
}
