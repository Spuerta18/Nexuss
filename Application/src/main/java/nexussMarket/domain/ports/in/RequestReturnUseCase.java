package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.User;

public interface RequestReturnUseCase {

    record Command(String returnRequestId, String orderId, String reason) {}

    ReturnRequest execute(User actor, Command command);
}
