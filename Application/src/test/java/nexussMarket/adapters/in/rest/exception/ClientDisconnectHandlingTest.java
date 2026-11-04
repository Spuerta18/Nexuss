package nexussMarket.adapters.in.rest.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import nexussMarket.adapters.in.rest.responses.ErrorResponse;

/**
 * A client that disconnects before the response is written is not an error:
 * {@link GlobalExceptionHandler} logs it at most at {@code DEBUG}, in one line
 * without a stack trace, and writes no response. Any other failure, including
 * an {@link AsyncRequestNotUsableException} that does not come from an aborted
 * connection, is still answered with {@code 500} and logged as an error.
 */
class ClientDisconnectHandlingTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/buyer/orders");
    private final Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ListAppender<ILoggingEvent> events = new ListAppender<>();
    private Level previousLevel;

    @BeforeEach
    void captureLogs() {
        previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        events.start();
        logger.addAppender(events);
    }

    @AfterEach
    void releaseLogs() {
        logger.detachAppender(events);
        logger.setLevel(previousLevel);
    }

    @Test
    void anAbortedConnectionWritesNothingAndLogsOneDebugLine() {
        ResponseHandled result = handle(new ClientAbortException(new IOException("An established connection\n"
                + "was aborted by the software in your host machine")));

        assertNull(result.response(), "no response is written for a client that is gone");
        assertOnlyOneDebugLine();
    }

    @Test
    void anUnusableAsyncRequestCausedByAnAbortIsNotAnError() {
        ResponseHandled result = handle(new AsyncRequestNotUsableException("Response not usable",
                new ClientAbortException("Broken pipe")));

        assertNull(result.response());
        assertOnlyOneDebugLine();
    }

    @Test
    void aDisconnectionWrappedInAnotherExceptionIsNotAnErrorEither() {
        ResponseHandled result = handle(new HttpMessageNotWritableException("Could not write JSON",
                new ClientAbortException("Connection reset by peer")));

        assertNull(result.response());
        assertOnlyOneDebugLine();
    }

    @Test
    void anUnusableAsyncRequestWithAnotherCauseIsStillAnError() {
        ResponseHandled result = handle(new AsyncRequestNotUsableException("Response not usable",
                new IOException("Disk full")));

        assertNotNull(result.response());
        assertEquals(500, result.response().getStatusCode().value());
        assertErrorLogged();
    }

    @Test
    void anyOtherFailureIsStillAnError() {
        ResponseHandled result = handle(new IllegalStateException("Something really broke"));

        assertEquals(500, result.response().getStatusCode().value());
        assertErrorLogged();
    }

    @Test
    void nothingIsLoggedForADisconnectionWhenDebugIsOff() {
        logger.setLevel(Level.INFO);

        handle(new ClientAbortException("Broken pipe"));

        assertTrue(events.list.isEmpty());
    }

    private record ResponseHandled(org.springframework.http.ResponseEntity<ErrorResponse> response) {
    }

    private ResponseHandled handle(Exception ex) {
        if (ex instanceof ClientAbortException || ex instanceof AsyncRequestNotUsableException) {
            return new ResponseHandled(handler.handleClientDisconnect(ex, request));
        }
        return new ResponseHandled(handler.handleUnexpected(ex, request));
    }

    private void assertOnlyOneDebugLine() {
        List<ILoggingEvent> logged = events.list;
        assertEquals(1, logged.size(), () -> "logged: " + logged);
        ILoggingEvent event = logged.get(0);
        assertEquals(Level.DEBUG, event.getLevel());
        assertNull(event.getThrowableProxy(), "no stack trace");
        assertFalse(event.getFormattedMessage().contains("\n"), "a single line");
        assertTrue(event.getFormattedMessage().contains("/api/buyer/orders"));
    }

    private void assertErrorLogged() {
        assertTrue(events.list.stream().anyMatch(event -> event.getLevel() == Level.ERROR
                && event.getThrowableProxy() != null), "a real failure keeps its ERROR log and stack trace");
    }
}
