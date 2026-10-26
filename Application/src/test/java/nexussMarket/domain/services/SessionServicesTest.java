package nexussMarket.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;

import nexussMarket.domain.exceptions.IncorrectCurrentPasswordException;
import nexussMarket.domain.exceptions.InvalidPasswordException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.UserStatus;

/** {@link LogoutService} and {@link ChangeUserPasswordService}. */
class SessionServicesTest extends AuditedServiceTestSupport {

    private static final String CURRENT = "Current-Pass-1";
    private static final String NEW = "New-Pass-123";

    private final UserRepositoryPort userRepositoryPort = mock(UserRepositoryPort.class);
    private final PasswordHasherPort passwordHasherPort = new PasswordHasherPort() {
        @Override
        public String hash(String rawPassword) {
            return "hash:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return passwordHash.equals("hash:" + rawPassword);
        }
    };

    private final LogoutService logoutService = new LogoutService(userRepositoryPort, audit);
    private final ChangeUserPasswordService changePasswordService =
            new ChangeUserPasswordService(userRepositoryPort, passwordHasherPort, validateStatus, audit);

    /** The stored user; the services receive a separate reference with the same identifier. */
    private final Seller stored = seller();

    @BeforeEach
    void storedUser() {
        stored.setPasswordHash("hash:" + CURRENT);
        when(userRepositoryPort.findById("S-1")).thenReturn(Optional.of(stored));
        when(userRepositoryPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
    }

    @Test
    void logoutRaisesTheStoredVersionAndIsAudited() {
        logoutService.execute(seller());

        assertEquals(1, stored.getAuthTokenVersion());
        verify(userRepositoryPort).save(stored);
        AuditLog entry = assertAudited(OperationType.USER_LOGOUT, stored, AffectedEntityType.USER, "S-1");
        assertEquals(0, entry.getDetails().size());
    }

    @Test
    void logoutIsIdempotent() {
        logoutService.execute(seller());
        logoutService.execute(seller());

        assertEquals(2, stored.getAuthTokenVersion());
        verify(userRepositoryPort, times(2)).save(stored);
    }

    @Test
    void passwordChangeStoresTheNewHashRevokesTokensAndIsAudited() {
        changePasswordService.execute(seller(), CURRENT, NEW);

        assertEquals("hash:" + NEW, stored.getPasswordHash());
        assertEquals(1, stored.getAuthTokenVersion());
        verify(userRepositoryPort).save(stored);
        AuditLog entry = assertAudited(OperationType.PASSWORD_CHANGE, stored, AffectedEntityType.USER, "S-1");
        assertEquals(false, entry.getDetails().containsValue(NEW));
        assertEquals(false, entry.getDetails().containsValue(CURRENT));
    }

    @Test
    void wrongCurrentPasswordChangesNothing() {
        assertThrows(IncorrectCurrentPasswordException.class,
                () -> changePasswordService.execute(seller(), "Wrong-Pass-1", NEW));

        assertUnchanged();
    }

    @Test
    void newPasswordNeedsEightCharacters() {
        assertThrows(InvalidPasswordException.class, () -> changePasswordService.execute(seller(), CURRENT, "Short1"));

        assertUnchanged();
    }

    @Test
    void onlyActiveUsersChangeTheirPassword() {
        Seller blocked = new Seller("S-1", "Sam Seller", "seller@example.com", UserStatus.BLOCKED, SellerStatus.ACTIVE);
        blocked.setPasswordHash("hash:" + CURRENT);
        when(userRepositoryPort.findById("S-1")).thenReturn(Optional.of(blocked));

        assertThrows(OperationNotAllowedException.class, () -> changePasswordService.execute(seller(), CURRENT, NEW));

        verify(userRepositoryPort, never()).save(any());
        assertNothingAudited();
    }

    private void assertUnchanged() {
        assertEquals("hash:" + CURRENT, stored.getPasswordHash());
        assertEquals(0, stored.getAuthTokenVersion());
        verify(userRepositoryPort, never()).save(any());
        assertNothingAudited();
    }
}
