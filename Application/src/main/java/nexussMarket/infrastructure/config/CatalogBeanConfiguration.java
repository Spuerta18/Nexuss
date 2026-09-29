package nexussMarket.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nexussMarket.domain.ports.in.ConsultProductUseCase;
import nexussMarket.domain.ports.in.ListPublishedProductsUseCase;
import nexussMarket.domain.ports.in.ListSellerProductsUseCase;
import nexussMarket.domain.ports.in.PublishProductUseCase;
import nexussMarket.domain.ports.in.UpdateProductStatusUseCase;
import nexussMarket.domain.ports.in.UpdateProductVariantsUseCase;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.services.ConsultProductService;
import nexussMarket.domain.services.ListPublishedProductsService;
import nexussMarket.domain.services.ListSellerProductsService;
import nexussMarket.domain.services.PublishProductService;
import nexussMarket.domain.services.UpdateProductStatusService;
import nexussMarket.domain.services.UpdateProductVariantsService;
import nexussMarket.domain.services.authorization.AuthorizeProductOwnershipService;
import nexussMarket.domain.services.authorization.ValidateUserAuthorizationStatusService;

/**
 * Use cases of the Catalog area: publishing and updating products, and
 * catalog queries. See {@link AuthorizationBeanConfiguration} for how the
 * domain wiring is split.
 */
@Configuration
public class CatalogBeanConfiguration {

    @Bean
    public PublishProductUseCase publishProductUseCase(ProductRepositoryPort productRepositoryPort,
                                                       UserRepositoryPort userRepositoryPort) {
        return new PublishProductService(productRepositoryPort, userRepositoryPort);
    }

    @Bean
    public UpdateProductStatusUseCase updateProductStatusUseCase(ProductRepositoryPort productRepositoryPort,
                                                                 AuthorizeProductOwnershipService authorizeProductOwnershipService) {
        return new UpdateProductStatusService(productRepositoryPort, authorizeProductOwnershipService);
    }

    @Bean
    public UpdateProductVariantsUseCase updateProductVariantsUseCase(ProductRepositoryPort productRepositoryPort,
                                                                     AuthorizeProductOwnershipService authorizeProductOwnershipService) {
        return new UpdateProductVariantsService(productRepositoryPort, authorizeProductOwnershipService);
    }

    @Bean
    public ListPublishedProductsUseCase listPublishedProductsUseCase(
            ProductRepositoryPort productRepositoryPort,
            ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        return new ListPublishedProductsService(productRepositoryPort, validateUserAuthorizationStatusService);
    }

    @Bean
    public ConsultProductUseCase consultProductUseCase(
            ProductRepositoryPort productRepositoryPort,
            ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService,
            AuthorizeProductOwnershipService authorizeProductOwnershipService) {
        return new ConsultProductService(productRepositoryPort, validateUserAuthorizationStatusService,
                authorizeProductOwnershipService);
    }

    @Bean
    public ListSellerProductsUseCase listSellerProductsUseCase(
            ProductRepositoryPort productRepositoryPort,
            ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        return new ListSellerProductsService(productRepositoryPort, validateUserAuthorizationStatusService);
    }
}
