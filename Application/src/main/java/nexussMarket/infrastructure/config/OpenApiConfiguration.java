package nexussMarket.infrastructure.config;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import nexussMarket.infrastructure.security.SecurityConfig;

/**
 * OpenAPI document served by springdoc at {@code /v3/api-docs} and rendered
 * by the Swagger UI at {@code /swagger-ui.html}. Every operation requires the
 * {@value #BEARER_SCHEME} scheme, so the UI's Authorize button sends
 * {@code Authorization: Bearer <token>}; login and buyer self-registration
 * are marked as public.
 */
@Configuration
public class OpenApiConfiguration {

    /** Name of the JWT security scheme in the OpenAPI document. */
    public static final String BEARER_SCHEME = "bearerAuth";

    private static final List<String> PUBLIC_PATHS =
            List.of(SecurityConfig.LOGIN_PATH, SecurityConfig.BUYER_REGISTRATION_PATH);

    @Bean
    public OpenAPI nexusOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NexusMarket API")
                        .description("Role-based REST API. Log in with POST " + SecurityConfig.LOGIN_PATH
                                + ", then paste the accessToken into Authorize.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    /** Removes the token requirement from the operations that do not need one. */
    @Bean
    public OpenApiCustomizer publicOperationsCustomizer() {
        return openApi -> PUBLIC_PATHS.stream()
                .map(path -> openApi.getPaths() == null ? null : openApi.getPaths().get(path))
                .filter(pathItem -> pathItem != null)
                .map(PathItem::readOperations)
                .flatMap(List::stream)
                .forEach(operation -> operation.setSecurity(List.of()));
    }
}
