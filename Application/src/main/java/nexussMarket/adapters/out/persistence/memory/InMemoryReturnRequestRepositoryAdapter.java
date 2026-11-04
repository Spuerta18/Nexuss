package nexussMarket.adapters.out.persistence.memory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.ReturnRequestDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.ReturnRequestMapper;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.ports.out.ReturnRequestRepositoryPort;

/** {@link ReturnRequestRepositoryPort} on the in-memory {@code return_requests} collection (one per order, versioned). */
@Repository
@Profile("memory")
public class InMemoryReturnRequestRepositoryAdapter implements ReturnRequestRepositoryPort {

    private final InMemoryDatabase database;
    private final InMemoryReferenceResolver referenceResolver;

    public InMemoryReturnRequestRepositoryAdapter(InMemoryDatabase database,
                                                  InMemoryReferenceResolver referenceResolver) {
        this.database = database;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public ReturnRequest insert(ReturnRequest returnRequest) {
        database.returnRequests.insert(ReturnRequestMapper.toDocument(returnRequest), "A return request with id "
                + returnRequest.getIdentifier() + ", or one for order " + returnRequest.getOrder().getIdentifier()
                + ", already exists");
        return returnRequest;
    }

    @Override
    public ReturnRequest save(ReturnRequest returnRequest) {
        ReturnRequestDocument saved = database.returnRequests.save(ReturnRequestMapper.toDocument(returnRequest),
                "Return request " + returnRequest.getIdentifier());
        returnRequest.restoreVersion(saved.getVersion());
        return returnRequest;
    }

    @Override
    public Optional<ReturnRequest> findById(String identifier) {
        return database.returnRequests.findById(identifier)
                .map(document -> referenceResolver.toReturnRequests(List.of(document)).get(0));
    }

    @Override
    public Optional<ReturnRequest> findByOrderId(String orderId) {
        return database.returnRequests.find(document -> Objects.equals(orderId, document.getOrderId())).stream()
                .findFirst().map(document -> referenceResolver.toReturnRequests(List.of(document)).get(0));
    }
}
