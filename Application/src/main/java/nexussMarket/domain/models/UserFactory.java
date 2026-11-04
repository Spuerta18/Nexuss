package nexussMarket.domain.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.DomainCatalog;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

/**
 * Central registry of the {@link User} specializations, keyed by the code of
 * their {@link SystemRole}. Adapters build users only through it (persistence
 * mappers, REST references), so a new kind of user is added by registering
 * one {@link UserType} here, without touching any mapper.
 *
 * <p>Fields that only some types have (a buyer's addresses, a seller's
 * status) travel as named <em>attributes</em>: strings, or lists of strings
 * for multi-valued ones. Catalog values travel as their code.</p>
 */
public final class UserFactory {

    /** Buyer: default delivery address. */
    public static final String PRIMARY_ADDRESS = "primaryAddress";
    /** Buyer: secondary delivery addresses (a list). */
    public static final String ADDITIONAL_ADDRESSES = "additionalAddresses";
    /** Buyer: {@link CustomerStatus} code. */
    public static final String COMMERCIAL_STATUS = "commercialStatus";
    /** Seller: {@link SellerStatus} code. */
    public static final String SELLER_STATUS = "sellerStatus";

    private static final Map<String, UserType<?>> TYPES = new ConcurrentHashMap<>();

    static {
        register(new UserType<>(SystemRole.BUYER, Buyer.class, data -> {
            Buyer buyer = new Buyer(data.identifier(), data.fullName(), data.email(), data.status(),
                    catalog(CustomerStatus::fromCode, data.attribute(COMMERCIAL_STATUS), "CustomerStatus"));
            buyer.setPrimaryAddress(data.attribute(PRIMARY_ADDRESS));
            buyer.setAdditionalAddresses(new ArrayList<>(data.listAttribute(ADDITIONAL_ADDRESSES)));
            return buyer;
        }, buyer -> attributes(PRIMARY_ADDRESS, buyer.getPrimaryAddress(),
                ADDITIONAL_ADDRESSES, List.copyOf(buyer.getAdditionalAddresses()),
                COMMERCIAL_STATUS, code(buyer.getCommercialStatus()))));
        register(new UserType<>(SystemRole.SELLER, Seller.class,
                data -> new Seller(data.identifier(), data.fullName(), data.email(), data.status(),
                        catalog(SellerStatus::fromCode, data.attribute(SELLER_STATUS), "SellerStatus")),
                seller -> attributes(SELLER_STATUS, code(seller.getSellerStatus()))));
        register(new UserType<>(SystemRole.LOGISTICS_OPERATOR, LogisticsOperator.class,
                data -> new LogisticsOperator(data.identifier(), data.fullName(), data.email(), data.status()),
                operator -> Map.of()));
        register(new UserType<>(SystemRole.ADMINISTRATOR, Administrator.class,
                data -> new Administrator(data.identifier(), data.fullName(), data.email(), data.status()),
                administrator -> Map.of()));
        register(new UserType<>(SystemRole.SUPERVISOR, Supervisor.class,
                data -> new Supervisor(data.identifier(), data.fullName(), data.email(), data.status()),
                supervisor -> Map.of()));
    }

    private UserFactory() {
    }

    /**
     * Registers a kind of user. Registering the same class again for its role
     * is a no-op; a role already taken by another class is rejected.
     */
    public static void register(UserType<?> type) {
        UserType<?> previous = TYPES.putIfAbsent(type.role().getCode(), type);
        if (previous != null && previous.type() != type.type()) {
            throw new IllegalStateException("Role '" + type.role().getCode() + "' is already registered for "
                    + previous.type().getSimpleName() + " in UserFactory");
        }
    }

    /** Whether a user type is registered for {@code roleCode}. */
    public static boolean isRegistered(String roleCode) {
        return roleCode != null && TYPES.containsKey(roleCode);
    }

    /** The role registered under {@code roleCode}. Fails for an unknown role. */
    public static SystemRole role(String roleCode) {
        return typeOf(roleCode).role();
    }

    /** Builds a user of the type registered for {@code roleCode}. Fails for an unknown role. */
    public static User create(String roleCode, UserData data) {
        return typeOf(roleCode).create(data);
    }

    /** Builds a user of the type registered for {@code role}. Fails for an unknown role. */
    public static User create(SystemRole role, UserData data) {
        return create(role != null ? role.getCode() : null, data);
    }

    /**
     * A reference to an existing user of {@code role}: only the identifier is
     * set. Services load the real user and read nothing else.
     */
    public static User reference(SystemRole role, String identifier) {
        return create(role, UserData.reference(identifier));
    }

    /** The type-specific attributes of {@code user} (empty for types without any); {@code null} values are left out. */
    public static Map<String, Object> attributesOf(User user) {
        return typeOf(user.getRole().getCode()).attributesOf(user);
    }

    private static UserType<?> typeOf(String roleCode) {
        UserType<?> type = roleCode != null ? TYPES.get(roleCode) : null;
        if (type == null) {
            throw new IllegalStateException("No user type is registered for role '" + roleCode + "'. Register it"
                    + " with UserFactory.register(new UserFactory.UserType<>(...)) in"
                    + " nexussMarket.domain.models.UserFactory before creating or reading such users.");
        }
        return type;
    }

    private static <T extends DomainCatalog> T catalog(Function<String, T> fromCode, String code, String catalog) {
        if (code == null) {
            return null;
        }
        T value = fromCode.apply(code);
        if (value == null) {
            throw new IllegalStateException("Unknown " + catalog + " code '" + code + "'");
        }
        return value;
    }

    private static String code(DomainCatalog value) {
        return value != null ? value.getCode() : null;
    }

    private static Map<String, Object> attributes(Object... namesAndValues) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            if (namesAndValues[i + 1] != null) {
                attributes.put((String) namesAndValues[i], namesAndValues[i + 1]);
            }
        }
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * One kind of user: its role, its class, how to build it from
     * {@link UserData}, and which type-specific attributes it exposes.
     */
    public static final class UserType<T extends User> {

        private final SystemRole role;
        private final Class<T> type;
        private final Function<UserData, T> factory;
        private final Function<T, Map<String, Object>> attributes;

        public UserType(SystemRole role, Class<T> type, Function<UserData, T> factory,
                        Function<T, Map<String, Object>> attributes) {
            this.role = Objects.requireNonNull(role, "role must not be null");
            this.type = Objects.requireNonNull(type, "type must not be null");
            this.factory = Objects.requireNonNull(factory, "factory must not be null");
            this.attributes = Objects.requireNonNull(attributes, "attributes must not be null");
        }

        public SystemRole role() {
            return role;
        }

        public Class<T> type() {
            return type;
        }

        T create(UserData data) {
            return factory.apply(data);
        }

        Map<String, Object> attributesOf(User user) {
            return attributes.apply(type.cast(user));
        }
    }

    /**
     * Everything needed to build a user: the fields every user has, plus the
     * type-specific {@code attributes} (see the constants of {@link UserFactory}).
     */
    public record UserData(String identifier, String fullName, String email, UserStatus status,
                           Map<String, Object> attributes) {

        public UserData {
            Map<String, Object> copy = new LinkedHashMap<>();
            if (attributes != null) {
                attributes.forEach((name, value) -> {
                    if (value != null) {
                        copy.put(name, value);
                    }
                });
            }
            attributes = Collections.unmodifiableMap(copy);
        }

        /** Only an identifier: the data of a reference. */
        public static UserData reference(String identifier) {
            return new UserData(identifier, null, null, null, Map.of());
        }

        /** A single-valued attribute, or {@code null} when absent. */
        public String attribute(String name) {
            Object value = attributes.get(name);
            return value != null ? value.toString() : null;
        }

        /** A multi-valued attribute, or an empty list when absent. */
        public List<String> listAttribute(String name) {
            Object value = attributes.get(name);
            if (value == null) {
                return List.of();
            }
            if (value instanceof List<?> list) {
                return list.stream().map(String::valueOf).toList();
            }
            return List.of(value.toString());
        }
    }
}
