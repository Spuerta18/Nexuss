package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.LogisticsOperator;

public interface RegisterLogisticsOperatorUseCase {

    record Command(String identifier, String fullName, String email) {}

    LogisticsOperator execute(Command command);
}
