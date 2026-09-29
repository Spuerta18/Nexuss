package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.models.Refund;

public interface RefundRepositoryPort {

    Refund save(Refund refund);

    Optional<Refund> findById(String identifier);

    Optional<Refund> findByReturnRequestId(String returnRequestId);
}
