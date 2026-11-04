package nexussMarket.domain.services;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.reports.LogisticsReport;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.ReportPeriod;
import nexussMarket.domain.valueobjects.SystemRole;

/** Logistics report: shipments created in a period, per status. Read-only; administrators and supervisors only. */
@Service
public class LogisticsReportService {

    private final ShipmentRepositoryPort shipmentRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public LogisticsReportService(ShipmentRepositoryPort shipmentRepositoryPort,
                                  ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.shipmentRepositoryPort = shipmentRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public LogisticsReport execute(User actor, ReportPeriod period) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        List<Shipment> shipments = shipmentRepositoryPort.findAllCreatedBetween(period.startInclusive(),
                period.endExclusive());
        Map<String, Long> byStatus = new TreeMap<>();
        shipments.forEach(shipment -> byStatus.merge(shipment.getStatus().getCode(), 1L, Long::sum));
        return new LogisticsReport(period, shipments.size(), byStatus);
    }
}
