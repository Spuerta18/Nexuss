package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.User;

public interface ApproveReturnUseCase {

    record Command(String returnRequestId) {}

    ReturnRequest execute(User actor, Command command);
}
