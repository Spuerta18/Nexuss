package nexussMarket.domain.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.models.reports.InventoryReport;
import nexussMarket.domain.models.reports.LowStockItem;
import nexussMarket.domain.models.reports.WarehouseStock;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Inventory report: current stock per warehouse, and every record of a
 * product still on sale (not discontinued) whose available units are at or
 * below {@code lowStockThreshold}, lowest first. A snapshot, not a period.
 * Read-only; administrators and supervisors only.
 */
@Service
public class InventoryReportService {

    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;
    public static final int MAX_LOW_STOCK_THRESHOLD = 100_000;

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public InventoryReportService(InventoryRepositoryPort inventoryRepositoryPort,
                                  ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.inventoryRepositoryPort = inventoryRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public InventoryReport execute(User actor, int lowStockThreshold) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        if (lowStockThreshold < 0 || lowStockThreshold > MAX_LOW_STOCK_THRESHOLD) {
            throw new IllegalArgumentException("lowStockThreshold must be between 0 and " + MAX_LOW_STOCK_THRESHOLD);
        }
        Map<String, long[]> totals = new LinkedHashMap<>();
        Map<String, Warehouse> warehouses = new LinkedHashMap<>();
        List<LowStockItem> lowStock = new ArrayList<>();
        for (InventoryItem item : inventoryRepositoryPort.findAll()) {
            Warehouse warehouse = item.getWarehouse();
            warehouses.putIfAbsent(warehouse.getIdentifier(), warehouse);
            long[] sums = totals.computeIfAbsent(warehouse.getIdentifier(), ignored -> new long[4]);
            sums[0]++;
            sums[1] += item.getAvailableQuantity();
            sums[2] += item.getReservedQuantity();
            sums[3] += item.getDamagedQuantity();
            if (item.getProduct().getStatus() != ProductStatus.DISCONTINUED
                    && item.getAvailableQuantity() <= lowStockThreshold) {
                lowStock.add(new LowStockItem(item.getProduct().getIdentifier(), item.getProduct().getName(),
                        warehouse.getIdentifier(), item.getAvailableQuantity()));
            }
        }
        List<WarehouseStock> stock = warehouses.values().stream()
                .map(warehouse -> {
                    long[] sums = totals.get(warehouse.getIdentifier());
                    return new WarehouseStock(warehouse.getIdentifier(), warehouse.getName(), warehouse.isActive(),
                            (int) sums[0], sums[1], sums[2], sums[3]);
                })
                .sorted(Comparator.comparing(WarehouseStock::warehouseId))
                .toList();
        lowStock.sort(Comparator.comparingInt(LowStockItem::availableUnits).thenComparing(LowStockItem::productId));
        return new InventoryReport(lowStockThreshold, stock, lowStock);
    }
}
