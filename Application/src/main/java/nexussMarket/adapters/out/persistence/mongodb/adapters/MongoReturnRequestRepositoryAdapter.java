package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.mappers.ReturnRequestMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.ReturnRequestMongoRepository;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.ports.out.ReturnRequestRepositoryPort;

/** Implements {@link ReturnRequestRepositoryPort} on the {@code return_requests} collection. */
@Repository
public class MongoReturnRequestRepositoryAdapter implements ReturnRequestRepositoryPort {

    private final ReturnRequestMongoRepository returnRequestRepository;
    private final MongoReferenceResolver referenceResolver;

    public MongoReturnRequestRepositoryAdapter(ReturnRequestMongoRepository returnRequestRepository,
                                               MongoReferenceResolver referenceResolver) {
        this.returnRequestRepository = returnRequestRepository;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public ReturnRequest save(ReturnRequest returnRequest) {
        returnRequestRepository.save(ReturnRequestMapper.toDocument(returnRequest));
        return returnRequest;
    }

    @Override
    public Optional<ReturnRequest> findById(String identifier) {
        return returnRequestRepository.findById(identifier)
                .map(document -> referenceResolver.toReturnRequests(List.of(document)).get(0));
    }

    @Override
    public Optional<ReturnRequest> findByOrderId(String orderId) {
        return returnRequestRepository.findByOrderId(orderId)
                .map(document -> referenceResolver.toReturnRequests(List.of(document)).get(0));
    }
}
