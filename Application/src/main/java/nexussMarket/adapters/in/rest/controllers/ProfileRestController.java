package nexussMarket.adapters.in.rest.controllers;

import java.util.Map;
import java.util.function.BiFunction;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import nexussMarket.adapters.in.rest.mappers.ProfileRestMapper;
import nexussMarket.adapters.in.rest.mappers.UserRestMapper;
import nexussMarket.adapters.in.rest.requests.UpdateProfileRequest;
import nexussMarket.adapters.in.rest.responses.UserResponse;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.AdministratorPort;
import nexussMarket.domain.ports.in.BuyerPort;
import nexussMarket.domain.ports.in.LogisticsOperatorPort;
import nexussMarket.domain.ports.in.SellerPort;
import nexussMarket.domain.ports.in.SupervisorPort;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * {@code PUT /api/me}: any authenticated user changes their own profile. It is
 * the one route shared by every role (like {@code GET /api/me}), so this
 * controller is the exception to "one controller per role port": it routes
 * the call to the caller's own role port, each of which declares
 * {@code updateProfile}. A role without a port mapping here gets {@code 403}.
 */
@RestController
@RequestMapping("/api/me")
public class ProfileRestController {

    private final Map<SystemRole, BiFunction<User, User, User>> profileUpdates;

    public ProfileRestController(BuyerPort buyerPort, SellerPort sellerPort, AdministratorPort administratorPort,
                                 LogisticsOperatorPort logisticsOperatorPort, SupervisorPort supervisorPort) {
        this.profileUpdates = Map.of(
                SystemRole.BUYER, buyerPort::updateProfile,
                SystemRole.SELLER, sellerPort::updateProfile,
                SystemRole.ADMINISTRATOR, administratorPort::updateProfile,
                SystemRole.LOGISTICS_OPERATOR, logisticsOperatorPort::updateProfile,
                SystemRole.SUPERVISOR, supervisorPort::updateProfile);
    }

    /** Name and, for buyers, addresses. Never email, role nor status. */
    @PutMapping
    public UserResponse updateProfile(@AuthenticationPrincipal(expression = "user") User actor,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        BiFunction<User, User, User> update = profileUpdates.get(actor.getRole());
        if (update == null) {
            throw new AccessDeniedException("Role " + actor.getRole().getCode() + " has no profile operation");
        }
        return UserRestMapper.toResponse(update.apply(actor, ProfileRestMapper.changes(actor, request)));
    }
}
