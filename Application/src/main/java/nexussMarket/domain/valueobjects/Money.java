package nexussMarket.domain.valueobjects;

import java.math.BigDecimal;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import nexussMarket.domain.exceptions.DomainException;

/**
 * An amount of money in a given {@link Currency}. Immutable and compared by
 * value; the amount is never negative.
 */
@Getter
@EqualsAndHashCode
public final class Money {

    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new DomainException("Money amount must not be null");
        }
        if (currency == null) {
            throw new DomainException("Money currency must not be null");
        }
        if (amount.signum() < 0) {
            throw new DomainException("Money amount must not be negative: " + amount);
        }
        this.amount = amount;
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    /** Fails with {@link DomainException} if the result would be negative. */
    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    private void requireSameCurrency(Money other) {
        if (other == null) {
            throw new DomainException("Money operand must not be null");
        }
        if (!currency.equals(other.currency)) {
            throw new DomainException("Cannot combine " + currency.getCode() + " with " + other.currency.getCode());
        }
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCode();
    }
}
