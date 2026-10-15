package nexussMarket.domain.models;

import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/**
 * A physical location where product inventory is managed. Classified by
 * ownership: Marketplace-owned or Seller-owned. A Seller-owned warehouse
 * references exactly one {@link Seller} as its owner.
 *
 * <p>A warehouse starts active and only changes through {@link #deactivate()}.</p>
 */
public class Warehouse {

    private final String identifier;
    private final String name;
    private final String address;
    private final WarehouseOwnerType ownerType;
    private final Seller owner;
    private boolean active;

    /** A reference or a new, active warehouse without owner. */
    public Warehouse(String identifier, String name, String address, WarehouseOwnerType ownerType) {
        this(identifier, name, address, ownerType, null, true);
    }

    /** A complete warehouse, e.g. one being registered or loaded from persistence. */
    public Warehouse(String identifier, String name, String address, WarehouseOwnerType ownerType, Seller owner,
                     boolean active) {
        this.identifier = identifier;
        this.name = name;
        this.address = address;
        this.ownerType = ownerType;
        this.owner = owner;
        this.active = active;
    }

    /** Unique identifier of the warehouse. */
    public String getIdentifier() {
        return identifier;
    }

    /** Descriptive name of the warehouse. */
    public String getName() {
        return name;
    }

    /** Physical location of the warehouse. */
    public String getAddress() {
        return address;
    }

    /** Classification of ownership (Marketplace or Seller). */
    public WarehouseOwnerType getOwnerType() {
        return ownerType;
    }

    /** Owning seller, present only when the owner type is {@code SELLER}. */
    public Seller getOwner() {
        return owner;
    }

    /** Whether the warehouse is currently operational. */
    public boolean isActive() {
        return active;
    }

    /**
     * Takes the warehouse out of operation: it can no longer receive inbound
     * stock and is skipped when reserving stock. Deactivating an inactive
     * warehouse has no effect.
     */
    public void deactivate() {
        active = false;
    }
}
