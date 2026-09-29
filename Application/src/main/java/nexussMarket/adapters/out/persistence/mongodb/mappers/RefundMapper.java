package nexussMarket.adapters.out.persistence.mongodb.mappers;

import nexussMarket.adapters.out.persistence.mongodb.documents.RefundDocument;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.valueobjects.RefundStatus;

/**
 * Converts {@link Refund} to and from {@link RefundDocument}. The return
 * request is stored as {@code returnRequestId} and must be resolved by the
 * caller.
 */
public final class RefundMapper {

    private RefundMapper() {
    }

    public static RefundDocument toDocument(Refund refund) {
        RefundDocument document = new RefundDocument();
        document.setId(refund.getIdentifier());
        document.setReturnRequestId(refund.getReturnRequest().getIdentifier());
        document.setAmount(refund.getAmount());
        document.setStatus(refund.getStatus().getCode());
        document.setCreatedAt(refund.getCreatedAt());
        document.setProcessedAt(refund.getProcessedAt());
        return document;
    }

    /** {@code returnRequest} is the return request referenced by {@code returnRequestId}. */
    public static Refund toDomain(RefundDocument document, ReturnRequest returnRequest) {
        return new Refund(document.getId(), returnRequest, document.getAmount(),
                CatalogCodes.resolve(RefundStatus::fromCode, document.getStatus(), "RefundStatus"),
                document.getCreatedAt(), document.getProcessedAt());
    }
}
