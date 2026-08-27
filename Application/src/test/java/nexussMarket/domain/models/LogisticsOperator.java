package nexussMarket.domain.models;

import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

/**
 * A user responsible for the physical operation of warehouses and dispatches.
 * Operates on {@link Warehouse} and {@link Order} entities during the dispatch
 * and shipping steps of the order lifecycle.
 */
public class LogisticsOperator extends User {

    public LogisticsOperator(String identifier, String fullName, String email, UserStatus status) {
        super(identifier, fullName, email, SystemRole.LOGISTICS_OPERATOR, status);
    }
}