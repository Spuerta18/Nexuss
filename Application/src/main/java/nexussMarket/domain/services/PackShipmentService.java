package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/** Marks a shipment's goods as packed. The order is not affected. */
@Service
public class PackShipmentService {

    private final ShipmentRepositoryPort shipmentRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public PackShipmentService(ShipmentRepositoryPort shipmentRepositoryPort,
                               ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.shipmentRepositoryPort = shipmentRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public Shipment execute(User actor, Shipment shipmentReference) {
        String shipmentId = shipmentReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.LOGISTICS_OPERATOR, SystemRole.ADMINISTRATOR);
        Shipment shipment = shipmentRepositoryPort.findById(shipmentId)
                .orElseThrow(() -> new EntityNotFoundException("No shipment found with id " + shipmentId));
        shipment.pack();
        return shipmentRepositoryPort.save(shipment);
    }
}
