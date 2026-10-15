package nexussMarket.adapters.in.rest.mappers;

import nexussMarket.adapters.in.rest.exception.InvalidRequestException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Builds domain <em>references</em> from identifiers received in the URL or
 * the request body: models carrying only the identifier of an existing
 * entity. The Domain Services load the real entity and ignore every other
 * field (see {@code SDD/Domain/Input-ports.md}).
 */
public final class DomainReferences {

    private DomainReferences() {
    }

    public static Product product(String id) {
        return new Product(id, null, null, null);
    }

    public static ShoppingCart cart(String id) {
        return new ShoppingCart(id, null);
    }

    public static Order order(String id) {
        return new Order(id, null, null);
    }

    public static Warehouse warehouse(String id) {
        return new Warehouse(id, null, null, null);
    }

    public static Shipment shipment(String id) {
        return new Shipment(id, null);
    }

    public static ReturnRequest returnRequest(String id) {
        return new ReturnRequest(id, null, null, null);
    }

    public static Refund refund(String id) {
        return new Refund(id, null, null);
    }

    public static Seller seller(String id) {
        return new Seller(id, null, null, null, null);
    }

    public static Buyer buyer(String id) {
        return new Buyer(id, null, null, null, null);
    }

    /**
     * A reference to any user. {@code User} is abstract, so the subtype is
     * chosen from {@code role}, which the client sends with the request: an
     * identifier alone does not tell which subtype to build. The services only
     * read the identifier and load the real user, so a role that does not
     * match the stored one has no effect on the result.
     */
    public static User user(String id, SystemRole role) {
        if (role == SystemRole.BUYER) {
            return buyer(id);
        }
        if (role == SystemRole.SELLER) {
            return seller(id);
        }
        if (role == SystemRole.LOGISTICS_OPERATOR) {
            return new LogisticsOperator(id, null, null, null);
        }
        if (role == SystemRole.ADMINISTRATOR) {
            return new Administrator(id, null, null, null);
        }
        if (role == SystemRole.SUPERVISOR) {
            return new Supervisor(id, null, null, null);
        }
        throw new InvalidRequestException("A user reference needs a role");
    }
}
