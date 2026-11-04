package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.mappers.ReturnRequestMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.ReturnRequestMongoRepository;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.ports.out.ReturnRequestRepositoryPort;

/** Implements {@link ReturnRequestRepositoryPort} on the {@code return_requests} collection. */
@Repository
@Profile("!memory")
public class MongoReturnRequestRepositoryAdapter implements ReturnRequestRepositoryPort {

    private final ReturnRequestMongoRepository returnRequestRepository;
    private final MongoReferenceResolver referenceResolver;

    public MongoReturnRequestRepositoryAdapter(ReturnRequestMongoRepository returnRequestRepository,
                                               MongoReferenceResolver referenceResolver) {
        this.returnRequestRepository = returnRequestRepository;
        this.referenceResolver = referenceResolver;
    }

    /** A real {@code insert}: an existing document with the same key is never replaced. */
    @Override
    public ReturnRequest insert(ReturnRequest returnRequest) {
        MongoInsertion.insert(() -> returnRequestRepository.insert(ReturnRequestMapper.toDocument(returnRequest)),
                "A return request with id " + returnRequest.getIdentifier()
                + ", or one for order " + returnRequest.getOrder().getIdentifier() + ", already exists");
        return returnRequest;
    }

    @Override
    public ReturnRequest save(ReturnRequest returnRequest) {
        returnRequest.restoreVersion(MongoVersioning.save(() -> returnRequestRepository.save(ReturnRequestMapper.toDocument(returnRequest)),
                "Return request " + returnRequest.getIdentifier()).getVersion());
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
