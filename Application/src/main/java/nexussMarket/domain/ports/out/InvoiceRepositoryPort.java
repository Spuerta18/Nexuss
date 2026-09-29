package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.models.Invoice;

public interface InvoiceRepositoryPort {

    Invoice save(Invoice invoice);

    Optional<Invoice> findByOrderId(String orderId);
}
