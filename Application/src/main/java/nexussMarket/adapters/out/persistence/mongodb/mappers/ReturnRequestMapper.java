package nexussMarket.adapters.out.persistence.mongodb.mappers;

import nexussMarket.adapters.out.persistence.mongodb.documents.ReturnRequestDocument;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.User;
import nexussMarket.domain.valueobjects.ReturnStatus;

/**
 * Converts {@link ReturnRequest} to and from {@link ReturnRequestDocument}.
 * The order, the buyer and the deciding user are stored as IDs and must be
 * resolved by the caller.
 */
public final class ReturnRequestMapper {

    private ReturnRequestMapper() {
    }

    public static ReturnRequestDocument toDocument(ReturnRequest returnRequest) {
        ReturnRequestDocument document = new ReturnRequestDocument();
        document.setId(returnRequest.getIdentifier());
        document.setOrderId(returnRequest.getOrder().getIdentifier());
        document.setBuyerId(returnRequest.getBuyer().getIdentifier());
        document.setReason(returnRequest.getReason());
        document.setStatus(returnRequest.getStatus().getCode());
        document.setRequestedAt(returnRequest.getRequestedAt());
        document.setDecidedAt(returnRequest.getDecidedAt());
        document.setDecidedById(returnRequest.getDecidedBy() != null ? returnRequest.getDecidedBy().getIdentifier() : null);
        return document;
    }

    /**
     * {@code order} and {@code buyer} are the entities referenced by the document;
     * {@code decidedBy} is the user referenced by {@code decidedById}, or
     * {@code null} while the request is undecided.
     */
    public static ReturnRequest toDomain(ReturnRequestDocument document, Order order, Buyer buyer, User decidedBy) {
        return new ReturnRequest(document.getId(), order, buyer, document.getReason(),
                CatalogCodes.resolve(ReturnStatus::fromCode, document.getStatus(), "ReturnStatus"),
                document.getRequestedAt(), document.getDecidedAt(), decidedBy);
    }
}
