package nexussMarket.adapters.in.rest.mappers;

import java.util.Map;

import nexussMarket.adapters.in.rest.requests.RegisterBuyerRequest;
import nexussMarket.adapters.in.rest.requests.RegisterStaffRequest;
import nexussMarket.adapters.in.rest.responses.AuthResponse;
import nexussMarket.adapters.in.rest.responses.UserResponse;
import nexussMarket.domain.models.AuthenticationResult;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.UserFactory;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

/**
 * Converts user requests into new domain users and domain users into
 * {@link UserResponse}. The status fields of a new user are placeholders: the
 * registration services set the real initial status.
 */
public final class UserRestMapper {

    public static final String TOKEN_TYPE = "Bearer";

    private UserRestMapper() {
    }

    public static Buyer newBuyer(RegisterBuyerRequest request) {
        Buyer buyer = new Buyer(request.identifier(), request.fullName(), request.email(), UserStatus.ACTIVE,
                CustomerStatus.ENABLED);
        buyer.setPrimaryAddress(request.primaryAddress());
        return buyer;
    }

    public static Seller newSeller(RegisterStaffRequest request) {
        return new Seller(request.identifier(), request.fullName(), request.email(), UserStatus.ACTIVE,
                SellerStatus.ACTIVE);
    }

    public static Supervisor newSupervisor(RegisterStaffRequest request) {
        return new Supervisor(request.identifier(), request.fullName(), request.email(), UserStatus.ACTIVE);
    }

    public static LogisticsOperator newLogisticsOperator(RegisterStaffRequest request) {
        return new LogisticsOperator(request.identifier(), request.fullName(), request.email(), UserStatus.ACTIVE);
    }

    /** Resolves the role code sent with requests about a user by identifier. */
    public static SystemRole role(String code) {
        return RestCodes.resolve(SystemRole::fromCode, code, "role");
    }

    public static UserStatus userStatus(String code) {
        return RestCodes.resolve(UserStatus::fromCode, code, "user status");
    }

    public static SellerStatus sellerStatus(String code) {
        return RestCodes.resolve(SellerStatus::fromCode, code, "seller status");
    }

    public static UserResponse toResponse(User user) {
        Map<String, Object> attributes = UserFactory.attributesOf(user);
        return new UserResponse(user.getIdentifier(), user.getFullName(), user.getEmail(),
                RestCodes.code(user.getRole()), RestCodes.code(user.getStatus()),
                (String) attributes.get(UserFactory.COMMERCIAL_STATUS),
                (String) attributes.get(UserFactory.PRIMARY_ADDRESS),
                (String) attributes.get(UserFactory.SELLER_STATUS));
    }

    public static AuthResponse toResponse(AuthenticationResult result) {
        return new AuthResponse(result.getToken(), TOKEN_TYPE, toResponse(result.getUser()));
    }
}
