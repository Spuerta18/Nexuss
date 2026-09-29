package nexussMarket.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import nexussMarket.domain.ports.out.AuditLogPort;
import nexussMarket.domain.services.AuditOperationService;

/**
 * Stateless JWT security: no HTTP session, no form login, no HTTP Basic and
 * no CSRF (tokens travel in the {@code Authorization} header, not in
 * cookies). Only login and buyer self-registration are public; every other
 * route requires a valid token. Unauthenticated requests get {@code 401};
 * role checks are left to {@code @PreAuthorize} on each endpoint.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /** Public endpoint where users exchange their credentials for a token. */
    public static final String LOGIN_PATH = "/api/auth/login";

    /** Public endpoint where buyers register themselves. */
    public static final String BUYER_REGISTRATION_PATH = "/api/auth/register";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtProvider jwtProvider,
                                                   NexusUserDetailsService userDetailsService,
                                                   AuditOperationService auditOperationService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, LOGIN_PATH, BUYER_REGISTRATION_PATH).permitAll()
                        // Lets Spring's error page answer with the real status instead of a 401.
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, userDetailsService, auditOperationService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Domain service used by {@link JwtAuthenticationFilter}. Registered here
     * because the security layer is its only consumer; the rest of the domain
     * wiring belongs to {@code infrastructure/config}.
     */
    @Bean
    public AuditOperationService auditOperationService(AuditLogPort auditLogPort) {
        return new AuditOperationService(auditLogPort);
    }
}
