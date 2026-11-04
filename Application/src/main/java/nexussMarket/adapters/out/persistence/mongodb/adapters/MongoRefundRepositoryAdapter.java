package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.RefundDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.RefundMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.RefundMongoRepository;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.ports.out.RefundRepositoryPort;

/** Implements {@link RefundRepositoryPort} on the {@code refunds} collection. */
@Repository
@Profile("!memory")
public class MongoRefundRepositoryAdapter implements RefundRepositoryPort {

    private final RefundMongoRepository refundRepository;
    private final MongoReferenceResolver referenceResolver;

    public MongoRefundRepositoryAdapter(RefundMongoRepository refundRepository,
                                        MongoReferenceResolver referenceResolver) {
        this.refundRepository = refundRepository;
        this.referenceResolver = referenceResolver;
    }

    /** A real {@code insert}: an existing document with the same key is never replaced. */
    @Override
    public Refund insert(Refund refund) {
        MongoInsertion.insert(() -> refundRepository.insert(RefundMapper.toDocument(refund)),
                "A refund with id " + refund.getIdentifier()
                + ", or one for return request " + refund.getReturnRequest().getIdentifier() + ", already exists");
        return refund;
    }

    @Override
    public Refund save(Refund refund) {
        refund.restoreVersion(MongoVersioning.save(() -> refundRepository.save(RefundMapper.toDocument(refund)),
                "Refund " + refund.getIdentifier()).getVersion());
        return refund;
    }

    @Override
    public Optional<Refund> findById(String identifier) {
        return refundRepository.findById(identifier).map(this::toDomain);
    }

    @Override
    public Optional<Refund> findByReturnRequestId(String returnRequestId) {
        return refundRepository.findByReturnRequestId(returnRequestId).map(this::toDomain);
    }

    private Refund toDomain(RefundDocument document) {
        return RefundMapper.toDomain(document, referenceResolver.returnRequest(document.getReturnRequestId()));
    }
}
