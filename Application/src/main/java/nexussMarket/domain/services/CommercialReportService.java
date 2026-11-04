package nexussMarket.domain.services;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.reports.CommercialReport;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.ReportPeriod;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Commercial report of the orders created in a period: count, count per
 * status, and sales of the paid ones per currency and per product. Read-only;
 * administrators and supervisors only.
 */
@Service
public class CommercialReportService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public CommercialReportService(OrderRepositoryPort orderRepositoryPort,
                                   ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public CommercialReport execute(User actor, ReportPeriod period) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        List<Order> orders = orderRepositoryPort.findAllCreatedBetween(period.startInclusive(), period.endExclusive());
        Map<String, Long> byStatus = new TreeMap<>();
        SalesFigures sales = new SalesFigures();
        long sold = 0;
        for (Order order : orders) {
            byStatus.merge(order.getStatus().getCode(), 1L, Long::sum);
            if (SalesFigures.sold(order)) {
                sold++;
                order.getLines().forEach(sales::add);
            }
        }
        return new CommercialReport(period, orders.size(), byStatus, sold, sales.revenueByCurrency(),
                sales.byProduct());
    }
}
