package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.models.ReturnRequest;

public interface ReturnRequestRepositoryPort {

    ReturnRequest save(ReturnRequest returnRequest);

    Optional<ReturnRequest> findById(String identifier);

    Optional<ReturnRequest> findByOrderId(String orderId);
}
