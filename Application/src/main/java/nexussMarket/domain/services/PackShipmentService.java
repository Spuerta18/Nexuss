package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.ShipmentStatus;
import nexussMarket.domain.valueobjects.SystemRole;

/** Marks a shipment's goods as packed. The order is not affected. */
@Service
public class PackShipmentService {

    private final ShipmentRepositoryPort shipmentRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public PackShipmentService(ShipmentRepositoryPort shipmentRepositoryPort,
                               ValidateRoleAuthorizationService validateRoleAuthorizationService,
                               RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.shipmentRepositoryPort = shipmentRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public Shipment execute(User actor, Shipment shipmentReference) {
        String shipmentId = shipmentReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.LOGISTICS_OPERATOR, SystemRole.ADMINISTRATOR);
        Shipment shipment = shipmentRepositoryPort.findById(shipmentId)
                .orElseThrow(() -> new EntityNotFoundException("No shipment found with id " + shipmentId));
        ShipmentStatus previousStatus = shipment.getStatus();
        shipment.pack();
        Shipment saved = shipmentRepositoryPort.save(shipment);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.SHIPMENT_PACKING, actor, AffectedEntityType.SHIPMENT, shipmentId),
                AuditDetails.of("previousStatus", previousStatus.getCode(),
                        "newStatus", shipment.getStatus().getCode()));
        return saved;
    }
}
