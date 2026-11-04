package nexussMarket.adapters.in.rest.exception;

import java.io.IOException;

/**
 * A request body grew past the configured limit while it was being read
 * (a body sent without {@code Content-Length}). Answered with {@code 413}.
 */
public class RequestBodyTooLargeException extends IOException {

    public RequestBodyTooLargeException(long limit) {
        super("Request body is larger than " + limit + " bytes");
    }
}
