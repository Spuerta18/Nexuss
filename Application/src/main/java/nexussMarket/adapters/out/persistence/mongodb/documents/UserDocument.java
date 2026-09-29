package nexussMarket.adapters.out.persistence.mongodb.documents;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Every {@code User} specialization, stored in the single {@code users}
 * collection. {@code userType} holds the {@code SystemRole} code and acts as
 * the discriminator; specialization-only fields are absent on documents of
 * other types.
 */
@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserDocument {

    @Id
    private String id;

    private String userType;
    private String fullName;

    @Indexed(unique = true)
    private String email;

    private String status;
    private String passwordHash;

    /** {@code BUYER} only. */
    private String primaryAddress;

    /** {@code BUYER} only. */
    private List<String> additionalAddresses;

    /** {@code BUYER} only. */
    private String commercialStatus;

    /** {@code SELLER} only. */
    private String sellerStatus;
}
