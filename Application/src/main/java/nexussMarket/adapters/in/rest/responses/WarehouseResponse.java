package nexussMarket.adapters.in.rest.responses;

/** A warehouse. The owning seller is exposed by identifier only; {@code null} for Marketplace-owned ones. */
public record WarehouseResponse(String identifier, String name, String address, String ownerType, String ownerId,
                                boolean active) {
}
