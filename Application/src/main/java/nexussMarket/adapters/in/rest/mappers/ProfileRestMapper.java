package nexussMarket.adapters.in.rest.mappers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nexussMarket.adapters.in.rest.exception.InvalidRequestException;
import nexussMarket.adapters.in.rest.requests.UpdateProfileRequest;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.UserFactory;
import nexussMarket.domain.models.UserFactory.UserData;

/** Builds the profile changes of {@code PUT /api/me}, of the same user type as the caller. */
public final class ProfileRestMapper {

    private ProfileRestMapper() {
    }

    /**
     * The changes requested by {@code actor}. Addresses left {@code null} keep the actor's current ones; only
     * buyers have addresses, so another role sending them gets {@code 400}.
     */
    public static User changes(User actor, UpdateProfileRequest request) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (actor instanceof Buyer buyer) {
            attributes.put(UserFactory.PRIMARY_ADDRESS, request.primaryAddress() != null
                    ? request.primaryAddress().trim() : buyer.getPrimaryAddress());
            attributes.put(UserFactory.ADDITIONAL_ADDRESSES, request.additionalAddresses() != null
                    ? request.additionalAddresses().stream().map(String::trim).toList()
                    : List.copyOf(buyer.getAdditionalAddresses()));
        } else if (request.primaryAddress() != null || request.additionalAddresses() != null) {
            throw new InvalidRequestException("Only buyers have addresses");
        }
        return UserFactory.create(actor.getRole(), new UserData(actor.getIdentifier(), request.fullName(), null,
                null, attributes));
    }
}
