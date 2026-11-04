package nexussMarket.domain.services;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.reports.SellerPerformanceReport;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.ReportPeriod;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Performance of one seller in a period, from the lines of their products in
 * the orders created then. Read-only. Administrators and supervisors may ask
 * for any seller; a seller always gets their own report, whatever seller the
 * request names.
 */
@Service
public class SellerPerformanceReportService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public SellerPerformanceReportService(OrderRepositoryPort orderRepositoryPort,
                                          ProductRepositoryPort productRepositoryPort,
                                          UserRepositoryPort userRepositoryPort,
                                          ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.productRepositoryPort = productRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public SellerPerformanceReport execute(User actor, Seller sellerReference, ReportPeriod period) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR,
                SystemRole.SELLER);
        // A seller only ever sees their own results.
        String sellerId = actor.getRole() == SystemRole.SELLER ? actor.getIdentifier() : sellerReference.getIdentifier();
        Seller seller = userRepositoryPort.findById(sellerId)
                .filter(Seller.class::isInstance).map(Seller.class::cast)
                .orElseThrow(() -> new EntityNotFoundException("No seller found with id " + sellerId));
        Set<String> products = productRepositoryPort.findBySellerId(sellerId).stream()
                .map(Product::getIdentifier).collect(Collectors.toSet());
        SalesFigures sales = new SalesFigures();
        long sold = 0;
        long pending = 0;
        long cancelled = 0;
        for (Order order : orderRepositoryPort.findAllCreatedBetween(period.startInclusive(), period.endExclusive())) {
            List<OrderLine> own = order.getLines().stream()
                    .filter(line -> products.contains(line.getProduct().getIdentifier())).toList();
            if (own.isEmpty()) {
                continue;
            }
            if (SalesFigures.sold(order)) {
                sold++;
                own.forEach(sales::add);
            } else if (order.getStatus() == OrderStatus.CANCELLED) {
                cancelled++;
            } else {
                pending++;
            }
        }
        return new SellerPerformanceReport(period, seller.getIdentifier(), seller.getFullName(), sold, sales.units(),
                sales.revenueByCurrency(), sales.byProduct(), pending, cancelled);
    }
}
