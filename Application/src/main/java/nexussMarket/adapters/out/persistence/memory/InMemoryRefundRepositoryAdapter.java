package nexussMarket.adapters.out.persistence.memory;

import java.util.Objects;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.RefundDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.RefundMapper;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.ports.out.RefundRepositoryPort;

/** {@link RefundRepositoryPort} on the in-memory {@code refunds} collection (one per return request, versioned). */
@Repository
@Profile("memory")
public class InMemoryRefundRepositoryAdapter implements RefundRepositoryPort {

    private final InMemoryDatabase database;
    private final InMemoryReferenceResolver referenceResolver;

    public InMemoryRefundRepositoryAdapter(InMemoryDatabase database, InMemoryReferenceResolver referenceResolver) {
        this.database = database;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public Refund insert(Refund refund) {
        database.refunds.insert(RefundMapper.toDocument(refund), "A refund with id " + refund.getIdentifier()
                + ", or one for return request " + refund.getReturnRequest().getIdentifier() + ", already exists");
        return refund;
    }

    @Override
    public Refund save(Refund refund) {
        RefundDocument saved = database.refunds.save(RefundMapper.toDocument(refund), "Refund " + refund.getIdentifier());
        refund.restoreVersion(saved.getVersion());
        return refund;
    }

    @Override
    public Optional<Refund> findById(String identifier) {
        return database.refunds.findById(identifier).map(this::toDomain);
    }

    @Override
    public Optional<Refund> findByReturnRequestId(String returnRequestId) {
        return database.refunds.find(document -> Objects.equals(returnRequestId, document.getReturnRequestId()))
                .stream().findFirst().map(this::toDomain);
    }

    private Refund toDomain(RefundDocument document) {
        return RefundMapper.toDomain(document, referenceResolver.returnRequest(document.getReturnRequestId()));
    }
}
