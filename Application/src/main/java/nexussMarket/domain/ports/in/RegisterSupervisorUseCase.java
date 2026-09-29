package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Supervisor;

public interface RegisterSupervisorUseCase {

    record Command(String identifier, String fullName, String email, String password) {}

    Supervisor execute(Command command);
}
