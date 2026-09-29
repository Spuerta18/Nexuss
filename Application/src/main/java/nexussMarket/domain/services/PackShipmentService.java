package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.PackShipmentUseCase;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/** Marks a shipment's goods as packed. The order is not affected. */
public class PackShipmentService implements PackShipmentUseCase {

    private final ShipmentRepositoryPort shipmentRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public PackShipmentService(ShipmentRepositoryPort shipmentRepositoryPort,
                               ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.shipmentRepositoryPort = shipmentRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    @Override
    public Shipment execute(User actor, Command command) {
        validateRoleAuthorizationService.execute(actor, SystemRole.LOGISTICS_OPERATOR, SystemRole.ADMINISTRATOR);
        Shipment shipment = shipmentRepositoryPort.findById(command.shipmentId())
                .orElseThrow(() -> new EntityNotFoundException("No shipment found with id " + command.shipmentId()));
        shipment.pack();
        return shipmentRepositoryPort.save(shipment);
    }
}
