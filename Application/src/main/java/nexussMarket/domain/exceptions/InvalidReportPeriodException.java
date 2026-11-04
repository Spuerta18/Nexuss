package nexussMarket.domain.exceptions;

/** A report period with {@code from} after {@code to}, or longer than allowed ({@code 400}). */
public class InvalidReportPeriodException extends DomainException {

    public InvalidReportPeriodException(String message) {
        super(message);
    }
}
