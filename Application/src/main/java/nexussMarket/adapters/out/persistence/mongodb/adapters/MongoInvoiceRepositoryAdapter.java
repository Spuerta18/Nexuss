package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.InvoiceDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.InvoiceMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.InvoiceMongoRepository;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Invoice;
import nexussMarket.domain.ports.out.InvoiceRepositoryPort;

/** Implements {@link InvoiceRepositoryPort} on the {@code invoices} collection. */
@Repository
public class MongoInvoiceRepositoryAdapter implements InvoiceRepositoryPort {

    private final InvoiceMongoRepository invoiceRepository;
    private final MongoReferenceResolver referenceResolver;

    public MongoInvoiceRepositoryAdapter(InvoiceMongoRepository invoiceRepository,
                                         MongoReferenceResolver referenceResolver) {
        this.invoiceRepository = invoiceRepository;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public Invoice save(Invoice invoice) {
        invoiceRepository.save(InvoiceMapper.toDocument(invoice));
        return invoice;
    }

    @Override
    public Optional<Invoice> findByOrderId(String orderId) {
        return invoiceRepository.findByOrderId(orderId).map(this::toDomain);
    }

    private Invoice toDomain(InvoiceDocument document) {
        return InvoiceMapper.toDomain(document, referenceResolver.order(document.getOrderId()),
                referenceResolver.user(document.getBuyerId(), Buyer.class));
    }
}
