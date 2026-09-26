package nexussMarket.domain.exceptions;

public class SellerNotAuthorizedException extends RuntimeException {

    public SellerNotAuthorizedException(String message) {
        super(message);
    }
}
