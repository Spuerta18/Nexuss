package nexussMarket.domain.models;

import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/**
 * A physical location where product inventory is managed. Classified by
 * ownership: Marketplace-owned or Seller-owned. A Seller-owned warehouse
 * references exactly one {@link Seller} as its owner.
 */
public class Warehouse {

    private String identifier;
    private String name;
    private String address;
    private WarehouseOwnerType ownerType;
    private Seller owner;

    public Warehouse(String identifier, String name, String address, WarehouseOwnerType ownerType) {
        this.identifier = identifier;
        this.name = name;
        this.address = address;
        this.ownerType = ownerType;
    }

    /** Unique identifier of the warehouse. */
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /** Descriptive name of the warehouse. */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** Physical location of the warehouse. */
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /** Classification of ownership (Marketplace or Seller). */
    public WarehouseOwnerType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(WarehouseOwnerType ownerType) {
        this.ownerType = ownerType;
    }

    /** Owning seller, present only when the owner type is {@code SELLER}. */
    public Seller getOwner() {
        return owner;
    }

    public void setOwner(Seller owner) {
        this.owner = owner;
    }
}