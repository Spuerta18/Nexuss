package nexussMarket.adapters.in.rest.mappers;

import nexussMarket.adapters.in.rest.requests.RequestReturnRequest;
import nexussMarket.adapters.in.rest.responses.RefundResponse;
import nexussMarket.adapters.in.rest.responses.ReturnRequestResponse;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.ReturnRequest;

/** Converts return requests into domain models, and return requests and refunds into responses. */
public final class ReturnRestMapper {

    private ReturnRestMapper() {
    }

    /** A new return request for the order in the URL; the service sets the buyer and the status. */
    public static ReturnRequest newReturnRequest(String orderId, RequestReturnRequest request) {
        return new ReturnRequest(request.returnRequestId(), DomainReferences.order(orderId), null, request.reason());
    }

    public static ReturnRequestResponse toResponse(ReturnRequest returnRequest) {
        return new ReturnRequestResponse(returnRequest.getIdentifier(),
                returnRequest.getOrder() != null ? returnRequest.getOrder().getIdentifier() : null,
                returnRequest.getBuyer() != null ? returnRequest.getBuyer().getIdentifier() : null,
                returnRequest.getReason(), RestCodes.code(returnRequest.getStatus()), returnRequest.getRequestedAt(),
                returnRequest.getDecidedAt(),
                returnRequest.getDecidedBy() != null ? returnRequest.getDecidedBy().getIdentifier() : null);
    }

    public static RefundResponse toResponse(Refund refund) {
        return new RefundResponse(refund.getIdentifier(),
                refund.getReturnRequest() != null ? refund.getReturnRequest().getIdentifier() : null,
                CatalogRestMapper.toResponse(refund.getAmount()), RestCodes.code(refund.getStatus()),
                refund.getCreatedAt(), refund.getProcessedAt());
    }
}
