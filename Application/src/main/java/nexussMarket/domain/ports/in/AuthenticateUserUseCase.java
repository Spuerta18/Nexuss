package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.User;

public interface AuthenticateUserUseCase {

    record Command(String email) {}

    User execute(Command command);
}
