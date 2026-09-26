package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.User;
import nexussMarket.domain.valueobjects.UserStatus;

public interface UpdateUserStatusUseCase {

    record Command(String userId, UserStatus status) {}

    User execute(Command command);
}
