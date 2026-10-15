package nexussMarket.infrastructure.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * CORS settings, bound from {@code nexus.cors.*}.
 *
 * @param allowedOrigins origins allowed to call the API from a browser, e.g.
 *                       {@code http://localhost:5173}. Supplied through the
 *                       {@code NEXUS_CORS_ORIGINS} environment variable as a
 *                       comma-separated list.
 */
@ConfigurationProperties(prefix = "nexus.cors")
public record CorsProperties(@DefaultValue({"http://localhost:3000", "http://localhost:5173"})
                             List<String> allowedOrigins) {
}
