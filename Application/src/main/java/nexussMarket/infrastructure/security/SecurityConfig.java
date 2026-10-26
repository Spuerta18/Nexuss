package nexussMarket.infrastructure.security;

import java.util.List;

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

/**
 * Stateless JWT security: no HTTP session, no form login, no HTTP Basic and
 * no CSRF (tokens travel in the {@code Authorization} header, not in
 * cookies). Only login, buyer self-registration and the API documentation
 * ({@link #API_DOCUMENTATION_PATHS}) are public; every other route requires a
 * valid token. Unauthenticated requests get {@code 401} and
 * requests rejected by a role check get {@code 403}, both with the uniform
 * error body; role checks are declared with {@code @PreAuthorize} on each
 * controller. CORS allows the origins configured in {@link CorsProperties}.
 *
 * <p><b>Logout.</b> Tokens cannot be deleted from the client's hands, so
 * logout revokes them instead: {@code POST /api/<role>/logout} (and
 * {@code POST /api/<role>/password}) raise the user's
 * {@code authTokenVersion}, and {@link JwtAuthenticationFilter} rejects every
 * token whose {@code ver} claim is older, answering {@code 401}. Spring
 * Security's own logout filter stays disabled: it clears an HTTP session,
 * which a stateless API does not have, and it would intercept the request
 * before the role controllers. {@link #SESSION_PATHS} require a valid token of
 * any role at this level; each controller then restricts them to its role.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtProvider jwtProvider,
                                                   NexusUserDetailsService userDetailsService) throws Exception {
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
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, LOGIN_PATH, BUYER_REGISTRATION_PATH).permitAll()
                        .requestMatchers(HttpMethod.GET, API_DOCUMENTATION_PATHS).permitAll()
                        .requestMatchers(HttpMethod.POST, SESSION_PATHS).authenticated()
                        // Lets Spring's error page answer with the real status instead of a 401.
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
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
