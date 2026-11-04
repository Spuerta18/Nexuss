package nexussMarket.infrastructure.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Limit on failed logins, counted separately per client address and per
 * attempted email ({@code nexus.security.login-throttle.*}). By default 10
 * failures within 5 minutes block that address or email for 1 minute.
 *
 * @param enabled       {@code false} turns the limit off (e.g. for a load test)
 * @param maxFailures   failures within {@code window} that trigger a block
 * @param window        sliding window in which failures are counted
 * @param blockDuration how long a blocked address or email gets {@code 429}
 */
@ConfigurationProperties(prefix = "nexus.security.login-throttle")
public record LoginThrottleProperties(@DefaultValue("true") boolean enabled,
                                      @DefaultValue("10") int maxFailures,
                                      @DefaultValue("5m") Duration window,
                                      @DefaultValue("1m") Duration blockDuration) {
}
