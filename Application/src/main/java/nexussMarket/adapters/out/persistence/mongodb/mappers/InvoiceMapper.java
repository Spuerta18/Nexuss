package nexussMarket.adapters.out.persistence.mongodb.mappers;

import nexussMarket.adapters.out.persistence.mongodb.documents.InvoiceDocument;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Invoice;
import nexussMarket.domain.models.Order;

/**
 * Converts {@link Invoice} to and from {@link InvoiceDocument}. The order and
 * the buyer are stored as IDs and must be resolved by the caller.
 */
public final class InvoiceMapper {

    private InvoiceMapper() {
    }

    public static InvoiceDocument toDocument(Invoice invoice) {
        InvoiceDocument document = new InvoiceDocument();
        document.setId(invoice.getIdentifier());
        document.setOrderId(invoice.getOrder().getIdentifier());
        document.setBuyerId(invoice.getBuyer().getIdentifier());
        document.setTotalAmount(invoice.getTotalAmount());
        document.setIssuedAt(invoice.getIssuedAt());
        return document;
    }

    /** {@code order} and {@code buyer} are the entities referenced by the document. */
    public static Invoice toDomain(InvoiceDocument document, Order order, Buyer buyer) {
        return new Invoice(document.getId(), order, buyer, document.getTotalAmount(), document.getIssuedAt());
    }
}
