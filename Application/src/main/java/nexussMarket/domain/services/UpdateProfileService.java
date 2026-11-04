package nexussMarket.domain.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateUserAuthorizationStatusService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;

/**
 * A user changes their own profile: the full name and, for a buyer, the
 * primary and additional addresses. Email, role, status and every credential
 * are never changed here. The stored user is loaded again and saved with its
 * version (409 on a concurrent change); the actor must be an active user
 * ({@link ValidateUserAuthorizationStatusService}). Audited as
 * {@code PROFILE_UPDATE} with the names of the changed fields only.
 */
@Service
public class UpdateProfileService {

    private final UserRepositoryPort userRepositoryPort;
    private final ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public UpdateProfileService(UserRepositoryPort userRepositoryPort,
                                ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService,
                                RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.userRepositoryPort = userRepositoryPort;
        this.validateUserAuthorizationStatusService = validateUserAuthorizationStatusService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    /**
     * {@code changes} carries the new full name and, when the actor is a buyer, the new addresses (a buyer
     * reference); any other field of it is ignored.
     */
    public User execute(User actor, User changes) {
        validateUserAuthorizationStatusService.execute(actor);
        String userId = actor.getIdentifier();
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("No user found with id " + userId));
        if (changes.getFullName() == null || changes.getFullName().isBlank()) {
            throw new IllegalArgumentException("The full name must not be blank");
        }
        List<String> changedFields = new ArrayList<>();
        String fullName = changes.getFullName().trim();
        if (!fullName.equals(user.getFullName())) {
            user.setFullName(fullName);
            changedFields.add("fullName");
        }
        if (user instanceof Buyer buyer && changes instanceof Buyer requested) {
            String primaryAddress = requested.getPrimaryAddress();
            if (primaryAddress != null && !primaryAddress.equals(buyer.getPrimaryAddress())) {
                buyer.setPrimaryAddress(primaryAddress);
                changedFields.add("primaryAddress");
            }
            List<String> additional = new ArrayList<>(requested.getAdditionalAddresses());
            if (!additional.equals(buyer.getAdditionalAddresses())) {
                buyer.setAdditionalAddresses(additional);
                changedFields.add("additionalAddresses");
            }
        }
        if (changedFields.isEmpty()) {
            return user;
        }
        User saved = userRepositoryPort.save(user);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.PROFILE_UPDATE, actor, AffectedEntityType.USER, userId),
                AuditDetails.of("changedFields", String.join(",", changedFields)));
        return saved;
    }
}
