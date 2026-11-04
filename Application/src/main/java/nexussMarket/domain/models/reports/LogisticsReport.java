package nexussMarket.domain.models.reports;

import java.util.Map;

import nexussMarket.domain.valueobjects.ReportPeriod;

/** Shipments created in a period, in total and per status. */
public record LogisticsReport(ReportPeriod period, long shipmentCount, Map<String, Long> shipmentsByStatus) {
}
