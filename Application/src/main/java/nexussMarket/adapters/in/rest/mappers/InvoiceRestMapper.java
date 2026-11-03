package nexussMarket.adapters.in.rest.mappers;

import nexussMarket.adapters.in.rest.responses.InvoiceResponse;
import nexussMarket.domain.models.Invoice;

/** Converts invoices into {@link InvoiceResponse}. */
public final class InvoiceRestMapper {

    private InvoiceRestMapper() {
    }

    public static InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(invoice.getIdentifier(),
                invoice.getOrder() != null ? invoice.getOrder().getIdentifier() : null,
                invoice.getBuyer() != null ? invoice.getBuyer().getIdentifier() : null,
                CatalogRestMapper.toResponse(invoice.getTotalAmount()), invoice.getIssuedAt());
    }
}
