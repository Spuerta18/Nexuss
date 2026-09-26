package nexussMarket.domain.models;

import java.util.ArrayList;
import java.util.List;

import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

/**
 * A user who purchases products published in the marketplace. A buyer never
 * manages information belonging to other buyers, nor manages inventory or
 * seller data.
 */
public class Buyer extends User {

    private String primaryAddress;
    private List<String> additionalAddresses = new ArrayList<>();
    private CustomerStatus commercialStatus;
    private List<ShoppingCart> carts = new ArrayList<>();
    private List<Order> orders = new ArrayList<>();

    public Buyer(String identifier, String fullName, String email, UserStatus status,
                 CustomerStatus commercialStatus) {
        super(identifier, fullName, email, SystemRole.BUYER, status);
        this.commercialStatus = commercialStatus;
    }

    /** Default delivery address. */
    public String getPrimaryAddress() {
        return primaryAddress;
    }

    public void setPrimaryAddress(String primaryAddress) {
        this.primaryAddress = primaryAddress;
    }

    /** Optional secondary delivery addresses. */
    public List<String> getAdditionalAddresses() {
        return additionalAddresses;
    }

    public void setAdditionalAddresses(List<String> additionalAddresses) {
        this.additionalAddresses = additionalAddresses != null ? additionalAddresses : new ArrayList<>();
    }

    /** Condition of the buyer with respect to making purchases. */
    public CustomerStatus getCommercialStatus() {
        return commercialStatus;
    }

    public void setCommercialStatus(CustomerStatus commercialStatus) {
        this.commercialStatus = commercialStatus;
    }

    /** Shopping carts belonging to the buyer (empty by default). */
    public List<ShoppingCart> getCarts() {
        return carts;
    }

    public void setCarts(List<ShoppingCart> carts) {
        this.carts = carts != null ? carts : new ArrayList<>();
    }

    /** Orders placed by the buyer (empty by default). */
    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders != null ? orders : new ArrayList<>();
    }
}