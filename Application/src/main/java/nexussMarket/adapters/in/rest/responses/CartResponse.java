package nexussMarket.adapters.in.rest.responses;

import java.util.List;

/** A shopping cart. */
public record CartResponse(String identifier, String buyerId, List<CartLineResponse> lines) {
}
