package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.SystemRole;

@Service
public class UpdateSellerStatusService {

    private final UserRepositoryPort userRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public UpdateSellerStatusService(UserRepositoryPort userRepositoryPort,
                                     ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                     RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.userRepositoryPort = userRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public Seller execute(User actor, Seller sellerReference, SellerStatus status) {
        String sellerId = sellerReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR);
        User user = userRepositoryPort.findById(sellerId)
                .orElseThrow(() -> new EntityNotFoundException("No seller found with id " + sellerId));
        if (!(user instanceof Seller seller)) {
            throw new EntityNotFoundException("No seller found with id " + sellerId);
        }
        SellerStatus previousStatus = seller.getSellerStatus();
        seller.setSellerStatus(status);
        Seller saved = (Seller) userRepositoryPort.save(seller);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.SELLER_STATUS_CHANGE, actor, AffectedEntityType.USER, sellerId),
                AuditDetails.of("previousStatus", previousStatus.getCode(),
                        "newStatus", seller.getSellerStatus().getCode()));
        return saved;
    }
}
