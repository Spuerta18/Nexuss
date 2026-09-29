package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.UpdateSellerStatusUseCase;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

public class UpdateSellerStatusService implements UpdateSellerStatusUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public UpdateSellerStatusService(UserRepositoryPort userRepositoryPort,
                                     ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.userRepositoryPort = userRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    @Override
    public Seller execute(User actor, Command command) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR);
        User user = userRepositoryPort.findById(command.sellerId())
                .orElseThrow(() -> new EntityNotFoundException("No seller found with id " + command.sellerId()));
        if (!(user instanceof Seller seller)) {
            throw new EntityNotFoundException("No seller found with id " + command.sellerId());
        }
        seller.setSellerStatus(command.status());
        return (Seller) userRepositoryPort.save(seller);
    }
}
