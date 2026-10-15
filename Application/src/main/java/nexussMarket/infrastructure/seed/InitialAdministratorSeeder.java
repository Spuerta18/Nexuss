package nexussMarket.infrastructure.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

/**
 * Creates the first {@link Administrator} at startup, so the platform can be
 * operated: every other staff account is registered by an administrator.
 *
 * <p>It only acts when both the email and the password are configured
 * ({@code NEXUS_ADMIN_EMAIL}, {@code NEXUS_ADMIN_PASSWORD}) and no
 * administrator exists yet, so it is idempotent: once an administrator exists,
 * later startups do nothing. The password is hashed through
 * {@link PasswordHasherPort} (BCrypt) and is never logged.</p>
 */
public class InitialAdministratorSeeder implements ApplicationRunner {

    /** Identifier given to the seeded administrator. */
    static final String IDENTIFIER = "INITIAL-ADMIN";
    static final String FULL_NAME = "Initial Administrator";

    private static final Logger log = LoggerFactory.getLogger(InitialAdministratorSeeder.class);

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final String email;
    private final String password;

    public InitialAdministratorSeeder(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort,
                                      String email, String password) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Checked before touching the database, so a startup without the variables never queries MongoDB.
        if (isBlank(email) || isBlank(password)) {
            log.info("Initial administrator not created: NEXUS_ADMIN_EMAIL and NEXUS_ADMIN_PASSWORD are not both set");
            return;
        }
        if (userRepositoryPort.existsByRole(SystemRole.ADMINISTRATOR)) {
            log.info("Initial administrator not created: an administrator already exists");
            return;
        }
        if (userRepositoryPort.existsByEmail(email) || userRepositoryPort.existsById(IDENTIFIER)) {
            log.warn("Initial administrator not created: the email {} or the identifier {} is already used by"
                    + " another user", email, IDENTIFIER);
            return;
        }
        Administrator administrator = new Administrator(IDENTIFIER, FULL_NAME, email, UserStatus.ACTIVE);
        administrator.setPasswordHash(passwordHasherPort.hash(password));
        userRepositoryPort.save(administrator);
        log.info("Initial administrator created with email {}", email);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
