package nexussMarket.adapters.out.persistence.mongodb.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import nexussMarket.adapters.out.persistence.mongodb.documents.InvoiceDocument;

/** Spring Data repository for the {@code invoices} collection. */
public interface InvoiceMongoRepository extends MongoRepository<InvoiceDocument, String> {

    Optional<InvoiceDocument> findByOrderId(String orderId);
}
