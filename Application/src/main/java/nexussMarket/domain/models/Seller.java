package nexussMarket.domain.models;

import java.util.ArrayList;
import java.util.List;

import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

/**
 * A user responsible for publishing and managing products in the marketplace.
 * Sellers cannot self-register; they are incorporated into the platform by an
 * {@link Administrator}.
 */
public class Seller extends User {

    private SellerStatus sellerStatus;
    private List<Warehouse> warehouses = new ArrayList<>();
    private List<Product> products = new ArrayList<>();

    public Seller(String identifier, String fullName, String email, UserStatus status,
                  SellerStatus sellerStatus) {
        super(identifier, fullName, email, SystemRole.SELLER, status);
        this.sellerStatus = sellerStatus;
    }

    /** Current operational status of the seller. */
    public SellerStatus getSellerStatus() {
        return sellerStatus;
    }

    public void setSellerStatus(SellerStatus sellerStatus) {
        this.sellerStatus = sellerStatus;
    }

    /** Warehouses owned by the seller (empty by default). */
    public List<Warehouse> getWarehouses() {
        return warehouses;
    }

    public void setWarehouses(List<Warehouse> warehouses) {
        this.warehouses = warehouses != null ? warehouses : new ArrayList<>();
    }

    /** Products published by the seller (empty by default). */
    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products != null ? products : new ArrayList<>();
    }
}