package nexussMarket.adapters.out.persistence.memory;

import java.util.Objects;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.InvoiceDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.InvoiceMapper;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Invoice;
import nexussMarket.domain.ports.out.InvoiceRepositoryPort;

/** {@link InvoiceRepositoryPort} on the in-memory {@code invoices} collection (one per order). */
@Repository
@Profile("memory")
public class InMemoryInvoiceRepositoryAdapter implements InvoiceRepositoryPort {

    private final InMemoryDatabase database;
    private final InMemoryReferenceResolver referenceResolver;

    public InMemoryInvoiceRepositoryAdapter(InMemoryDatabase database, InMemoryReferenceResolver referenceResolver) {
        this.database = database;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public Invoice insert(Invoice invoice) {
        database.invoices.insert(InvoiceMapper.toDocument(invoice),
                "An invoice with id " + invoice.getIdentifier() + " already exists");
        return invoice;
    }

    @Override
    public Optional<Invoice> findByOrderId(String orderId) {
        return database.invoices.find(document -> Objects.equals(orderId, document.getOrderId())).stream()
                .findFirst().map(this::toDomain);
    }

    private Invoice toDomain(InvoiceDocument document) {
        return InvoiceMapper.toDomain(document, referenceResolver.order(document.getOrderId()),
                referenceResolver.user(document.getBuyerId(), Buyer.class));
    }
}
