package nexussMarket.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;

import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.AuditLogRepositoryPort;
import nexussMarket.domain.ports.out.OperationRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeCartAccessService;
import nexussMarket.domain.services.authorization.AuthorizeOrderAccessService;
import nexussMarket.domain.services.authorization.AuthorizeProductOwnershipService;
import nexussMarket.domain.services.authorization.AuthorizeWarehouseOperationService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.services.authorization.ValidateUserAuthorizationStatusService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.UserStatus;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/**
 * Shared setup of the Domain Service tests that check auditing: the real
 * {@link RegisterOperationAndAuditService} and authorization services, with
 * the two audit ports mocked so each test can verify the {@link Operation} and
 * {@link AuditLog} a service records. Repository ports are mocked by each test.
 */
abstract class AuditedServiceTestSupport {

    protected final OperationRepositoryPort operationRepositoryPort = mock(OperationRepositoryPort.class);
    protected final AuditLogRepositoryPort auditLogRepositoryPort = mock(AuditLogRepositoryPort.class);
    protected final RegisterOperationAndAuditService audit = new RegisterOperationAndAuditService(
            new RegisterOperationService(operationRepositoryPort), new RegisterAuditLogService(auditLogRepositoryPort));

    protected final ValidateUserAuthorizationStatusService validateStatus = new ValidateUserAuthorizationStatusService();
    protected final ValidateRoleAuthorizationService validateRole = new ValidateRoleAuthorizationService(validateStatus);
    protected final AuthorizeCartAccessService authorizeCart = new AuthorizeCartAccessService(validateStatus);
    protected final AuthorizeOrderAccessService authorizeOrder = new AuthorizeOrderAccessService(validateStatus);
    protected final AuthorizeProductOwnershipService authorizeProduct = new AuthorizeProductOwnershipService(validateStatus);
    protected final AuthorizeWarehouseOperationService authorizeWarehouse =
            new AuthorizeWarehouseOperationService(validateStatus);

    @BeforeEach
    void auditPortsReturnWhatTheyStore() {
        when(operationRepositoryPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(auditLogRepositoryPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
    }

    /**
     * Verifies that exactly one operation and one audit log were recorded, both
     * describing {@code type} performed by {@code performer} on the given
     * entity, and that no detail names a credential. Returns the audit log.
     */
    protected AuditLog assertAudited(OperationType type, User performer, AffectedEntityType entityType,
                                     String entityId) {
        ArgumentCaptor<Operation> operation = ArgumentCaptor.forClass(Operation.class);
        ArgumentCaptor<AuditLog> auditLog = ArgumentCaptor.forClass(AuditLog.class);
        verify(operationRepositoryPort).save(operation.capture());
        verify(auditLogRepositoryPort).save(auditLog.capture());

        Operation recorded = operation.getValue();
        assertNotNull(recorded.getIdentifier());
        assertNotNull(recorded.getExecutionDate());
        assertEquals(type, recorded.getOperationType());
        assertSame(performer, recorded.getPerformedBy());
        assertEquals(entityType, recorded.getAffectedEntityType());
        assertEquals(entityId, recorded.getAffectedEntityId());

        AuditLog entry = auditLog.getValue();
        assertNotNull(entry.getAuditId());
        assertNotNull(entry.getOperationDate());
        assertEquals(type, entry.getOperationType());
        assertSame(performer, entry.getPerformedBy());
        assertEquals(performer.getRole(), entry.getUserRole());
        assertEquals(entityType, entry.getAffectedEntityType());
        assertEquals(entityId, entry.getAffectedEntityId());
        entry.getDetails().keySet().forEach(key -> assertFalse(key.toLowerCase(Locale.ROOT).contains("password"),
                "audit details must not hold credentials"));
        return entry;
    }

    protected void assertNothingAudited() {
        verifyNoInteractions(operationRepositoryPort, auditLogRepositoryPort);
    }

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

    protected static Money cop(long amount) {
        return Money.of(BigDecimal.valueOf(amount), Currency.COP);
    }

    protected static Product product(Seller seller) {
        return new Product("P-1", "Keyboard", ProductType.PHYSICAL, ProductStatus.PUBLISHED, seller, cop(1000),
                List.of());
    }

    protected static Warehouse warehouse(Seller owner) {
        return new Warehouse("W-1", "Main", "2 Dock Street", WarehouseOwnerType.SELLER, owner, true);
    }

    protected static Order order(Buyer buyer, OrderStatus status) {
        Product product = product(seller());
        return new Order("O-1", buyer, status,
                List.of(new OrderLine(product, product.getName(), 2, product.getPrice(), warehouse(seller()))));
    }
}
