package nexussMarket.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nexussMarket.domain.ports.in.ApproveReturnUseCase;
import nexussMarket.domain.ports.in.ConfirmDeliveryUseCase;
import nexussMarket.domain.ports.in.ConfirmInventoryOutboundUseCase;
import nexussMarket.domain.ports.in.CreateShipmentUseCase;
import nexussMarket.domain.ports.in.DispatchShipmentUseCase;
import nexussMarket.domain.ports.in.PackShipmentUseCase;
import nexussMarket.domain.ports.in.ProcessRefundUseCase;
import nexussMarket.domain.ports.in.RegisterInventoryReturnUseCase;
import nexussMarket.domain.ports.in.RejectReturnUseCase;
import nexussMarket.domain.ports.in.RequestReturnUseCase;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.RefundRepositoryPort;
import nexussMarket.domain.ports.out.ReturnRequestRepositoryPort;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;
import nexussMarket.domain.services.ApproveReturnService;
import nexussMarket.domain.services.ConfirmDeliveryService;
import nexussMarket.domain.services.CreateShipmentService;
import nexussMarket.domain.services.DispatchShipmentService;
import nexussMarket.domain.services.PackShipmentService;
import nexussMarket.domain.services.ProcessRefundService;
import nexussMarket.domain.services.RejectReturnService;
import nexussMarket.domain.services.RequestReturnService;
import nexussMarket.domain.services.authorization.AuthorizeOrderAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;

/**
 * Use cases after an order is paid: the shipment lifecycle, and returns and
 * refunds. See {@link AuthorizationBeanConfiguration} for how the domain
 * wiring is split.
 */
@Configuration
public class FulfillmentBeanConfiguration {

    @Bean
    public CreateShipmentUseCase createShipmentUseCase(ShipmentRepositoryPort shipmentRepositoryPort,
                                                       OrderRepositoryPort orderRepositoryPort,
                                                       ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new CreateShipmentService(shipmentRepositoryPort, orderRepositoryPort, validateRoleAuthorizationService);
    }

    @Bean
    public PackShipmentUseCase packShipmentUseCase(ShipmentRepositoryPort shipmentRepositoryPort,
                                                   ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new PackShipmentService(shipmentRepositoryPort, validateRoleAuthorizationService);
    }

    @Bean
    public DispatchShipmentUseCase dispatchShipmentUseCase(ShipmentRepositoryPort shipmentRepositoryPort,
                                                           OrderRepositoryPort orderRepositoryPort,
                                                           ConfirmInventoryOutboundUseCase confirmInventoryOutboundUseCase,
                                                           ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new DispatchShipmentService(shipmentRepositoryPort, orderRepositoryPort, confirmInventoryOutboundUseCase,
                validateRoleAuthorizationService);
    }

    @Bean
    public ConfirmDeliveryUseCase confirmDeliveryUseCase(ShipmentRepositoryPort shipmentRepositoryPort,
                                                         OrderRepositoryPort orderRepositoryPort,
                                                         ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new ConfirmDeliveryService(shipmentRepositoryPort, orderRepositoryPort, validateRoleAuthorizationService);
    }

    @Bean
    public RequestReturnUseCase requestReturnUseCase(ReturnRequestRepositoryPort returnRequestRepositoryPort,
                                                     OrderRepositoryPort orderRepositoryPort,
                                                     ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                                     AuthorizeOrderAccessService authorizeOrderAccessService) {
        return new RequestReturnService(returnRequestRepositoryPort, orderRepositoryPort,
                validateRoleAuthorizationService, authorizeOrderAccessService);
    }

    @Bean
    public ApproveReturnUseCase approveReturnUseCase(ReturnRequestRepositoryPort returnRequestRepositoryPort,
                                                     RefundRepositoryPort refundRepositoryPort,
                                                     RegisterInventoryReturnUseCase registerInventoryReturnUseCase,
                                                     ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new ApproveReturnService(returnRequestRepositoryPort, refundRepositoryPort,
                registerInventoryReturnUseCase, validateRoleAuthorizationService);
    }

    @Bean
    public RejectReturnUseCase rejectReturnUseCase(ReturnRequestRepositoryPort returnRequestRepositoryPort,
                                                   ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new RejectReturnService(returnRequestRepositoryPort, validateRoleAuthorizationService);
    }

    @Bean
    public ProcessRefundUseCase processRefundUseCase(RefundRepositoryPort refundRepositoryPort,
                                                     ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new ProcessRefundService(refundRepositoryPort, validateRoleAuthorizationService);
    }
}
