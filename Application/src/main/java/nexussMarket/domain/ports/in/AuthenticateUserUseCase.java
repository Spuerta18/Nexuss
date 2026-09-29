package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.AuthenticationResult;

public interface AuthenticateUserUseCase {

    record Command(String email, String password) {}

    AuthenticationResult execute(Command command);
}
