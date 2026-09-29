package nexussMarket.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nexussMarket.domain.ports.in.AddCartLineUseCase;
import nexussMarket.domain.ports.in.AdvanceOrderStatusUseCase;
import nexussMarket.domain.ports.in.CancelOrderUseCase;
import nexussMarket.domain.ports.in.ConfirmOrderUseCase;
import nexussMarket.domain.ports.in.ConsultCartUseCase;
import nexussMarket.domain.ports.in.ConsultOrderUseCase;
import nexussMarket.domain.ports.in.CreateShoppingCartUseCase;
import nexussMarket.domain.ports.in.ListBuyerOrdersUseCase;
import nexussMarket.domain.ports.in.ReleaseInventoryReservationUseCase;
import nexussMarket.domain.ports.in.RemoveCartLineUseCase;
import nexussMarket.domain.ports.in.UpdateCartLineQuantityUseCase;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.ports.out.InvoiceRepositoryPort;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.services.AddCartLineService;
import nexussMarket.domain.services.AdvanceOrderStatusService;
import nexussMarket.domain.services.CancelOrderService;
import nexussMarket.domain.services.ConfirmOrderService;
import nexussMarket.domain.services.ConsultCartService;
import nexussMarket.domain.services.ConsultOrderService;
import nexussMarket.domain.services.CreateShoppingCartService;
import nexussMarket.domain.services.ListBuyerOrdersService;
import nexussMarket.domain.services.RemoveCartLineService;
import nexussMarket.domain.services.ReserveInventoryService;
import nexussMarket.domain.services.UpdateCartLineQuantityService;
import nexussMarket.domain.services.authorization.AuthorizeCartAccessService;
import nexussMarket.domain.services.authorization.AuthorizeOrderAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.services.authorization.ValidateUserAuthorizationStatusService;

/**
 * Use cases of the Carts and Orders areas: cart management, order
 * confirmation, payment, cancellation, and cart/order queries. See
 * {@link AuthorizationBeanConfiguration} for how the domain wiring is split.
 */
@Configuration
public class CartOrderBeanConfiguration {

    @Bean
    public CreateShoppingCartUseCase createShoppingCartUseCase(CartRepositoryPort cartRepositoryPort,
                                                               UserRepositoryPort userRepositoryPort) {
        return new CreateShoppingCartService(cartRepositoryPort, userRepositoryPort);
    }

    @Bean
    public AddCartLineUseCase addCartLineUseCase(CartRepositoryPort cartRepositoryPort,
                                                 ProductRepositoryPort productRepositoryPort) {
        return new AddCartLineService(cartRepositoryPort, productRepositoryPort);
    }

    @Bean
    public UpdateCartLineQuantityUseCase updateCartLineQuantityUseCase(CartRepositoryPort cartRepositoryPort) {
        return new UpdateCartLineQuantityService(cartRepositoryPort);
    }

    @Bean
    public RemoveCartLineUseCase removeCartLineUseCase(CartRepositoryPort cartRepositoryPort) {
        return new RemoveCartLineService(cartRepositoryPort);
    }

    @Bean
    public ConsultCartUseCase consultCartUseCase(CartRepositoryPort cartRepositoryPort,
                                                 AuthorizeCartAccessService authorizeCartAccessService) {
        return new ConsultCartService(cartRepositoryPort, authorizeCartAccessService);
    }

    @Bean
    public ConfirmOrderUseCase confirmOrderUseCase(CartRepositoryPort cartRepositoryPort,
                                                   OrderRepositoryPort orderRepositoryPort,
                                                   ReserveInventoryService reserveInventoryService,
                                                   ReleaseInventoryReservationUseCase releaseInventoryReservationUseCase,
                                                   ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                                   AuthorizeCartAccessService authorizeCartAccessService) {
        return new ConfirmOrderService(cartRepositoryPort, orderRepositoryPort, reserveInventoryService,
                releaseInventoryReservationUseCase, validateRoleAuthorizationService, authorizeCartAccessService);
    }

    @Bean
    public AdvanceOrderStatusUseCase advanceOrderStatusUseCase(OrderRepositoryPort orderRepositoryPort,
                                                               InvoiceRepositoryPort invoiceRepositoryPort,
                                                               ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new AdvanceOrderStatusService(orderRepositoryPort, invoiceRepositoryPort,
                validateRoleAuthorizationService);
    }

    @Bean
    public CancelOrderUseCase cancelOrderUseCase(OrderRepositoryPort orderRepositoryPort,
                                                 ReleaseInventoryReservationUseCase releaseInventoryReservationUseCase,
                                                 ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                                 AuthorizeOrderAccessService authorizeOrderAccessService) {
        return new CancelOrderService(orderRepositoryPort, releaseInventoryReservationUseCase,
                validateRoleAuthorizationService, authorizeOrderAccessService);
    }

    @Bean
    public ConsultOrderUseCase consultOrderUseCase(OrderRepositoryPort orderRepositoryPort,
                                                   AuthorizeOrderAccessService authorizeOrderAccessService) {
        return new ConsultOrderService(orderRepositoryPort, authorizeOrderAccessService);
    }

    @Bean
    public ListBuyerOrdersUseCase listBuyerOrdersUseCase(
            OrderRepositoryPort orderRepositoryPort,
            ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        return new ListBuyerOrdersService(orderRepositoryPort, validateUserAuthorizationStatusService);
    }
}
