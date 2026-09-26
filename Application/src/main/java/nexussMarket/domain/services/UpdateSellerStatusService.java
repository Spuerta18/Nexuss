package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.UpdateSellerStatusUseCase;
import nexussMarket.domain.ports.out.UserRepositoryPort;

public class UpdateSellerStatusService implements UpdateSellerStatusUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public UpdateSellerStatusService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public Seller execute(Command command) {
        User user = userRepositoryPort.findById(command.sellerId())
                .orElseThrow(() -> new EntityNotFoundException("No seller found with id " + command.sellerId()));
        if (!(user instanceof Seller seller)) {
            throw new EntityNotFoundException("No seller found with id " + command.sellerId());
        }
        seller.setSellerStatus(command.status());
        return (Seller) userRepositoryPort.save(seller);
    }
}
