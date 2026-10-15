package nexussMarket.adapters.in.rest.responses;

import java.util.List;

/** A catalog product. The seller is exposed by identifier only. */
public record ProductResponse(String identifier, String name, String productType, String status, String sellerId,
                              MoneyResponse price, List<VariantResponse> variants) {
}
