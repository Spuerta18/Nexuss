package nexussMarket.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nexussMarket.domain.ports.in.AuthenticateUserUseCase;
import nexussMarket.domain.ports.in.ConsultUserUseCase;
import nexussMarket.domain.ports.in.RegisterBuyerUseCase;
import nexussMarket.domain.ports.in.RegisterLogisticsOperatorUseCase;
import nexussMarket.domain.ports.in.RegisterSellerUseCase;
import nexussMarket.domain.ports.in.RegisterSupervisorUseCase;
import nexussMarket.domain.ports.in.UpdateSellerStatusUseCase;
import nexussMarket.domain.ports.in.UpdateUserStatusUseCase;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.TokenServicePort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.services.AuthenticateUserService;
import nexussMarket.domain.services.ConsultUserService;
import nexussMarket.domain.services.RegisterBuyerService;
import nexussMarket.domain.services.RegisterLogisticsOperatorService;
import nexussMarket.domain.services.RegisterSellerService;
import nexussMarket.domain.services.RegisterSupervisorService;
import nexussMarket.domain.services.UpdateSellerStatusService;
import nexussMarket.domain.services.UpdateUserStatusService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;

/**
 * Use cases of the Users area: authentication, registration, status updates
 * and user queries. See {@link AuthorizationBeanConfiguration} for how the
 * domain wiring is split.
 */
@Configuration
public class UserBeanConfiguration {

    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(UserRepositoryPort userRepositoryPort,
                                                           PasswordHasherPort passwordHasherPort,
                                                           TokenServicePort tokenServicePort) {
        return new AuthenticateUserService(userRepositoryPort, passwordHasherPort, tokenServicePort);
    }

    @Bean
    public RegisterBuyerUseCase registerBuyerUseCase(UserRepositoryPort userRepositoryPort,
                                                     PasswordHasherPort passwordHasherPort) {
        return new RegisterBuyerService(userRepositoryPort, passwordHasherPort);
    }

    @Bean
    public RegisterSellerUseCase registerSellerUseCase(UserRepositoryPort userRepositoryPort,
                                                       PasswordHasherPort passwordHasherPort) {
        return new RegisterSellerService(userRepositoryPort, passwordHasherPort);
    }

    @Bean
    public RegisterLogisticsOperatorUseCase registerLogisticsOperatorUseCase(UserRepositoryPort userRepositoryPort,
                                                                             PasswordHasherPort passwordHasherPort) {
        return new RegisterLogisticsOperatorService(userRepositoryPort, passwordHasherPort);
    }

    @Bean
    public RegisterSupervisorUseCase registerSupervisorUseCase(UserRepositoryPort userRepositoryPort,
                                                               PasswordHasherPort passwordHasherPort) {
        return new RegisterSupervisorService(userRepositoryPort, passwordHasherPort);
    }

    @Bean
    public UpdateUserStatusUseCase updateUserStatusUseCase(UserRepositoryPort userRepositoryPort,
                                                           ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new UpdateUserStatusService(userRepositoryPort, validateRoleAuthorizationService);
    }

    @Bean
    public UpdateSellerStatusUseCase updateSellerStatusUseCase(UserRepositoryPort userRepositoryPort,
                                                               ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new UpdateSellerStatusService(userRepositoryPort, validateRoleAuthorizationService);
    }

    @Bean
    public ConsultUserUseCase consultUserUseCase(UserRepositoryPort userRepositoryPort,
                                                 ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new ConsultUserService(userRepositoryPort, validateRoleAuthorizationService);
    }
}
