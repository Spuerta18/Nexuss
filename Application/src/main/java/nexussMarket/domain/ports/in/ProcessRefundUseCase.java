package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.User;

public interface ProcessRefundUseCase {

    record Command(String refundId) {}

    Refund execute(User actor, Command command);
}
