package nexussMarket.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Size limit of request bodies ({@code nexus.http.max-request-body-size},
 * 1 MB by default); larger bodies get {@code 413}.
 */
@ConfigurationProperties(prefix = "nexus.http")
public record RequestLimitProperties(@DefaultValue("1MB") DataSize maxRequestBodySize) {
}
