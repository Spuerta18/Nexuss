package nexussMarket.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nexussMarket.domain.services.authorization.AuthorizeCartAccessService;
import nexussMarket.domain.services.authorization.AuthorizeOrderAccessService;
import nexussMarket.domain.services.authorization.AuthorizeProductOwnershipService;
import nexussMarket.domain.services.authorization.AuthorizeWarehouseOperationService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.services.authorization.ValidateUserAuthorizationStatusService;

/**
 * Domain wiring, split into one {@code @Configuration} class per area so each
 * stays readable (about sixty beans in total):
 * <ul>
 *   <li>{@code AuthorizationBeanConfiguration} — the authorization services, used by every area;</li>
 *   <li>{@link InternalServiceBeanConfiguration} — internal services without an input port;</li>
 *   <li>{@link UserBeanConfiguration}, {@link CatalogBeanConfiguration},
 *       {@link InventoryBeanConfiguration}, {@link CartOrderBeanConfiguration},
 *       {@link FulfillmentBeanConfiguration} — the use cases of each area.</li>
 * </ul>
 * Domain services are plain classes, so they are registered here instead of
 * being component-scanned. Use cases are exposed as their input port type, so
 * adapters and other services depend on the port, never on the concrete
 * service. Output port adapters are {@code @Repository} / {@code @Component}
 * and are found by component scanning.
 *
 * <p>This class: the six authorization services.</p>
 */
@Configuration
public class AuthorizationBeanConfiguration {

    @Bean
    public ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService() {
        return new ValidateUserAuthorizationStatusService();
    }

    @Bean
    public ValidateRoleAuthorizationService validateRoleAuthorizationService(
            ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        return new ValidateRoleAuthorizationService(validateUserAuthorizationStatusService);
    }

    @Bean
    public AuthorizeOrderAccessService authorizeOrderAccessService(
            ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        return new AuthorizeOrderAccessService(validateUserAuthorizationStatusService);
    }

    @Bean
    public AuthorizeProductOwnershipService authorizeProductOwnershipService(
            ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        return new AuthorizeProductOwnershipService(validateUserAuthorizationStatusService);
    }

    @Bean
    public AuthorizeWarehouseOperationService authorizeWarehouseOperationService(
            ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        return new AuthorizeWarehouseOperationService(validateUserAuthorizationStatusService);
    }

    @Bean
    public AuthorizeCartAccessService authorizeCartAccessService(
            ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        return new AuthorizeCartAccessService(validateUserAuthorizationStatusService);
    }
}
