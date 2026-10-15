package nexussMarket.domain.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

class UserTest {

    private static final List<String> IMMUTABLE_FIELDS = List.of("identifier", "email", "role");

    @Test
    void identityIsFixedByTheConstructor() {
        Administrator admin = new Administrator("A-1", "Root Admin", "admin@example.com", UserStatus.ACTIVE);

        assertEquals("A-1", admin.getIdentifier());
        assertEquals("admin@example.com", admin.getEmail());
        assertEquals(SystemRole.ADMINISTRATOR, admin.getRole());
    }

    @Test
    void identityFieldsAreFinal() throws NoSuchFieldException {
        for (String name : IMMUTABLE_FIELDS) {
            Field field = User.class.getDeclaredField(name);
            assertTrue(Modifier.isFinal(field.getModifiers()), name + " must be final");
        }
    }

    @Test
    void noSubclassExposesASetterForIdentity() {
        List<Class<? extends User>> types = List.of(User.class, Buyer.class, Seller.class, LogisticsOperator.class,
                Administrator.class, Supervisor.class);
        for (Class<? extends User> type : types) {
            List<String> setters = Arrays.stream(type.getMethods()).map(Method::getName).toList();
            for (String forbidden : List.of("setIdentifier", "setEmail", "setRole")) {
                assertTrue(!setters.contains(forbidden), type.getSimpleName() + " must not expose " + forbidden);
            }
        }
    }
}
