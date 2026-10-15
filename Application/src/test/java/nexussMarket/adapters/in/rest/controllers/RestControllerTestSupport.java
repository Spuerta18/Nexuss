package nexussMarket.adapters.in.rest.controllers;

import static org.mockito.Mockito.when;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.AdministratorPort;
import nexussMarket.domain.ports.in.BuyerPort;
import nexussMarket.domain.ports.in.LogisticsOperatorPort;
import nexussMarket.domain.ports.in.PublicAccessPort;
import nexussMarket.domain.ports.in.SellerPort;
import nexussMarket.domain.ports.in.SupervisorPort;
import nexussMarket.domain.ports.out.AuditLogPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.UserStatus;
import nexussMarket.infrastructure.security.JwtProvider;

/**
 * Shared setup of the REST controller tests: the full application context
 * with the real security (JWT filter, {@code @PreAuthorize}, CORS) and the
 * real controllers, mappers and exception handler, but with every role port
 * mocked. {@link UserRepositoryPort} is mocked so the JWT filter reloads the
 * users these tests define, and {@link AuditLogPort} so no audit record
 * reaches MongoDB. No MongoDB server is needed.
 *
 * <p>Every test class extends this one with the same mocks, so Spring reuses
 * a single application context for all of them.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class RestControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    protected PublicAccessPort publicAccessPort;

    @MockitoBean
    protected BuyerPort buyerPort;

    @MockitoBean
    protected SellerPort sellerPort;

    @MockitoBean
    protected AdministratorPort administratorPort;

    @MockitoBean
    protected LogisticsOperatorPort logisticsOperatorPort;

    @MockitoBean
    protected SupervisorPort supervisorPort;

    @MockitoBean
    protected UserRepositoryPort userRepositoryPort;

    @MockitoBean
    protected AuditLogPort auditLogPort;

    protected static Buyer buyer() {
        Buyer buyer = new Buyer("B-1", "Ana Buyer", "buyer@example.com", UserStatus.ACTIVE, CustomerStatus.ENABLED);
        buyer.setPrimaryAddress("1 Main Street");
        return buyer;
    }

    protected static Seller seller() {
        return new Seller("S-1", "Sam Seller", "seller@example.com", UserStatus.ACTIVE, SellerStatus.ACTIVE);
    }

    protected static Administrator administrator() {
        return new Administrator("A-1", "Alex Admin", "admin@example.com", UserStatus.ACTIVE);
    }

    protected static LogisticsOperator logisticsOperator() {
        return new LogisticsOperator("L-1", "Lee Logistics", "logistics@example.com", UserStatus.ACTIVE);
    }

    protected static Supervisor supervisor() {
        return new Supervisor("V-1", "Val Supervisor", "supervisor@example.com", UserStatus.ACTIVE);
    }

    /**
     * An {@code Authorization} header value for {@code user}, whose identifier
     * the JWT filter will resolve to {@code user} itself.
     */
    protected String bearer(User user) {
        when(userRepositoryPort.findById(user.getIdentifier())).thenReturn(Optional.of(user));
        return "Bearer " + jwtProvider.generateToken(user);
    }

    /** A token issued for {@code tokenUser}, without registering it in the repository. */
    protected String rawBearer(User tokenUser) {
        return "Bearer " + jwtProvider.generateToken(tokenUser);
    }

    protected static String authorization() {
        return HttpHeaders.AUTHORIZATION;
    }
}
