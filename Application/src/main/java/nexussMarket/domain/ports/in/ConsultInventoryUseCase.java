package nexussMarket.domain.ports.in;

import java.util.List;

import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.User;

public interface ConsultInventoryUseCase {

    record Query(String productId) {}

    List<InventoryItem> execute(User actor, Query query);
}
