package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Invoice;

public interface InvoiceRepositoryPort {

    /**
     * Stores a new invoice. Fails with {@link DuplicateResourceException} if an invoice with the same identifier
     * already exists; the stored one is never replaced.
     */
    Invoice insert(Invoice invoice);

    Optional<Invoice> findByOrderId(String orderId);
}
