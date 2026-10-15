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
 * Domain wiring for the services that are not component-scanned:
 * <ul>
 *   <li>{@code AuthorizationBeanConfiguration} — the authorization services, used by every area;</li>
 *   <li>{@link InternalServiceBeanConfiguration} — internal services without an input port.</li>
 * </ul>
 * Use case services ({@code domain/services}) are {@code @Service} classes and
 * the use case adapters ({@code adapters/useCases}) that implement the role
 * input ports are component-scanned, as are the output port adapters
 * ({@code @Repository} / {@code @Component}).
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
