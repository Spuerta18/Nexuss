package nexussMarket.domain.exceptions;

/** A page number or page size outside the allowed range ({@code 400}). */
public class InvalidPageRequestException extends DomainException {

    public InvalidPageRequestException(String message) {
        super(message);
    }
}
