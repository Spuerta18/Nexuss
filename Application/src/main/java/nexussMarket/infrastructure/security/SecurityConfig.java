package nexussMarket.infrastructure.security;

import java.time.Clock;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import nexussMarket.adapters.in.rest.exception.RestErrors;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Stateless JWT security: no HTTP session, no form login, no HTTP Basic and
 * no CSRF (tokens travel in the {@code Authorization} header, not in
 * cookies). Only login, buyer self-registration and the API documentation
 * ({@link #API_DOCUMENTATION_PATHS}) are public; every other route requires a
 * valid token. Unauthenticated requests get {@code 401} and
 * requests rejected by a role check get {@code 403}, both with the uniform
 * error body. CORS allows the origins configured in {@link CorsProperties}.
 *
 * <p><b>Authorization by path prefix.</b> Each role's routes live under its
 * own prefix ({@link #ROLE_PATHS}) and are restricted to that role here, in
 * the filter chain, before the request body is read or validated: a wrong
 * role gets {@code 403} even with an invalid body. {@code /api/me} needs any
 * valid token, and every route not listed is denied ({@code denyAll}). The
 * controllers keep their {@code @PreAuthorize}, a second check of the same
 * rule.</p>
 *
 * <p><b>Abuse limits.</b> {@link RequestBodyLimitFilter} answers {@code 413}
 * to bodies above {@code nexus.http.max-request-body-size}, and
 * {@link LoginThrottleFilter} answers {@code 429} to an address or email with
 * too many failed logins ({@link LoginThrottleProperties}).</p>
 *
 * <p><b>Logout.</b> Tokens cannot be deleted from the client's hands, so
 * logout revokes them instead: {@code POST /api/<role>/logout} (and
 * {@code POST /api/<role>/password}) raise the user's
 * {@code authTokenVersion}, and {@link JwtAuthenticationFilter} rejects every
 * token whose {@code ver} claim is older, answering {@code 401}. Spring
 * Security's own logout filter stays disabled: it clears an HTTP session,
 * which a stateless API does not have, and if it were mapped to these paths it
 * would answer before the role controllers, skipping the port. They fall under
 * each role's prefix, so only that role reaches them.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class, LoginThrottleProperties.class,
        RequestLimitProperties.class})
public class SecurityConfig {

    /** Public endpoint where users exchange their credentials for a token. */
    public static final String LOGIN_PATH = "/api/auth/login";

    /** Public endpoint where buyers register themselves. */
    public static final String BUYER_REGISTRATION_PATH = "/api/auth/register";

    /**
     * Public, read-only API documentation: the Swagger UI, its
     * {@code /swagger-ui.html} entry point (a redirect into
     * {@code /swagger-ui/}) and the OpenAPI document. Calling an endpoint
     * from the UI still needs a token (the Authorize button).
     */
    public static final String[] API_DOCUMENTATION_PATHS = {
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"};

    /** Logout and password change of every role: they revoke the caller's tokens. */
    public static final String[] SESSION_PATHS = {"/api/*/logout", "/api/*/password"};

    /** Any authenticated user: their own profile. */
    public static final String PROFILE_PATH = "/api/me";

    /** Route prefix of each role; only that role may call it. */
    public static final Map<String, SystemRole> ROLE_PATHS = rolePaths();

    private static Map<String, SystemRole> rolePaths() {
        Map<String, SystemRole> paths = new LinkedHashMap<>();
        paths.put("/api/buyer/**", SystemRole.BUYER);
        paths.put("/api/seller/**", SystemRole.SELLER);
        paths.put("/api/admin/**", SystemRole.ADMINISTRATOR);
        paths.put("/api/logistics/**", SystemRole.LOGISTICS_OPERATOR);
        paths.put("/api/supervisor/**", SystemRole.SUPERVISOR);
        return Collections.unmodifiableMap(paths);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtProvider jwtProvider,
                                                   NexusUserDetailsService userDetailsService,
                                                   LoginAttemptLimiter loginAttemptLimiter,
                                                   LoginThrottleProperties loginThrottleProperties,
                                                   RequestLimitProperties requestLimitProperties) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // Session-based logout is not used: logout revokes tokens (see the class Javadoc).
                .logout(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ex) -> RestErrors.write(response,
                                HttpStatus.UNAUTHORIZED, "Authentication required", request))
                        .accessDeniedHandler((request, response, ex) -> RestErrors.write(response,
                                HttpStatus.FORBIDDEN, "Access denied", request)))
                .authorizeHttpRequests(authorize -> {
                    authorize
                            .requestMatchers(HttpMethod.POST, LOGIN_PATH, BUYER_REGISTRATION_PATH).permitAll()
                            .requestMatchers(HttpMethod.GET, API_DOCUMENTATION_PATHS).permitAll()
                            // Lets Spring's error page answer with the real status instead of a 401.
                            .requestMatchers("/error").permitAll()
                            .requestMatchers(PROFILE_PATH).authenticated();
                    ROLE_PATHS.forEach((path, role) -> authorize.requestMatchers(path).hasRole(role.getCode()));
                    authorize.anyRequest().denyAll();
                });
        http.addFilterBefore(new RequestBodyLimitFilter(requestLimitProperties.maxRequestBodySize().toBytes()),
                UsernamePasswordAuthenticationFilter.class);
        if (loginThrottleProperties.enabled()) {
            http.addFilterBefore(new LoginThrottleFilter(loginAttemptLimiter), UsernamePasswordAuthenticationFilter.class);
        }
        http.addFilterBefore(new JwtAuthenticationFilter(jwtProvider, userDetailsService),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Failed-login counters, shared by every request of this instance. */
    @Bean
    public LoginAttemptLimiter loginAttemptLimiter(LoginThrottleProperties properties) {
        return new LoginAttemptLimiter(properties.maxFailures(), properties.window(), properties.blockDuration(),
                Clock.systemUTC());
    }

    /**
     * Browsers may call the API from the configured origins. The token travels
     * in the {@code Authorization} header, so credentials (cookies) are not
     * allowed.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE,
                RestErrors.REQUEST_ID_HEADER));
        configuration.setExposedHeaders(List.of(RestErrors.REQUEST_ID_HEADER));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
