package nexussMarket.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;

import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.UserStatus;

/** Registrations and status changes of users record their operation and audit log. */
class UserServicesAuditTest extends AuditedServiceTestSupport {

    private final UserRepositoryPort userRepositoryPort = mock(UserRepositoryPort.class);
    private final PasswordHasherPort passwordHasherPort = mock(PasswordHasherPort.class);
    private final Administrator admin = administrator();

    @BeforeEach
    void repositoriesReturnWhatTheyStore() {
        when(userRepositoryPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(passwordHasherPort.hash(any())).thenReturn("hashed");
    }

    @Test
    void buyerSelfRegistrationIsPerformedByTheNewBuyer() {
        Buyer saved = new RegisterBuyerService(userRepositoryPort, passwordHasherPort, audit)
                .execute(buyer(), "Secret-Pass-1");

        AuditLog entry = assertAudited(OperationType.BUYER_REGISTRATION, saved, AffectedEntityType.USER, "B-1");
        assertEquals("ACTIVE", entry.getDetails().get("newStatus"));
    }

    @Test
    void sellerRegistrationIsRecorded() {
        Seller saved = new RegisterSellerService(userRepositoryPort, passwordHasherPort, validateRole, audit)
                .execute(admin, seller(), "Secret-Pass-1");

        assertAudited(OperationType.SELLER_REGISTRATION, admin, AffectedEntityType.USER, saved.getIdentifier());
    }

    @Test
    void supervisorRegistrationIsRecorded() {
        Supervisor saved = new RegisterSupervisorService(userRepositoryPort, passwordHasherPort, validateRole, audit)
                .execute(admin, supervisor(), "Secret-Pass-1");

        assertAudited(OperationType.SUPERVISOR_REGISTRATION, admin, AffectedEntityType.USER, saved.getIdentifier());
    }

    @Test
    void logisticsOperatorRegistrationIsRecorded() {
        LogisticsOperator saved = new RegisterLogisticsOperatorService(userRepositoryPort, passwordHasherPort,
                validateRole, audit).execute(admin, logisticsOperator(), "Secret-Pass-1");

        assertAudited(OperationType.LOGISTICS_OPERATOR_REGISTRATION, admin, AffectedEntityType.USER,
                saved.getIdentifier());
    }

    @Test
    void userStatusChangeRecordsPreviousAndNewStatus() {
        Buyer target = buyer();
        when(userRepositoryPort.findById("B-1")).thenReturn(Optional.of(target));

        new UpdateUserStatusService(userRepositoryPort, validateRole, audit).execute(admin, target, UserStatus.BLOCKED);

        AuditLog entry = assertAudited(OperationType.USER_STATUS_CHANGE, admin, AffectedEntityType.USER, "B-1");
        assertEquals("ACTIVE", entry.getDetails().get("previousStatus"));
        assertEquals("BLOCKED", entry.getDetails().get("newStatus"));
    }

    @Test
    void sellerStatusChangeRecordsPreviousAndNewStatus() {
        Seller target = seller();
        when(userRepositoryPort.findById("S-1")).thenReturn(Optional.of(target));

        new UpdateSellerStatusService(userRepositoryPort, validateRole, audit)
                .execute(admin, target, SellerStatus.SUSPENDED);

        AuditLog entry = assertAudited(OperationType.SELLER_STATUS_CHANGE, admin, AffectedEntityType.USER, "S-1");
        assertEquals("ACTIVE", entry.getDetails().get("previousStatus"));
        assertEquals("SUSPENDED", entry.getDetails().get("newStatus"));
    }

    @Test
    void aRejectedOperationRecordsNothing() {
        Administrator blocked = new Administrator("A-2", "Blocked Admin", "blocked@example.com", UserStatus.BLOCKED);
        RegisterSellerService service =
                new RegisterSellerService(userRepositoryPort, passwordHasherPort, validateRole, audit);

        assertThrows(OperationNotAllowedException.class, () -> service.execute(blocked, seller(), "Secret-Pass-1"));

        assertNothingAudited();
    }
}
