package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.User;

public interface ConsultUserUseCase {

    record Query(String userId) {}

    User execute(User actor, Query query);
}
