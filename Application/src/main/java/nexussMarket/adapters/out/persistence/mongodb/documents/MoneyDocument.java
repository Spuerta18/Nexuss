package nexussMarket.adapters.out.persistence.mongodb.documents;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Embedded {@code Money}: the amount as {@code Decimal128} and the
 * {@code Currency} code.
 */
@Getter
@Setter
@NoArgsConstructor
public class MoneyDocument {

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    private String currency;
}
