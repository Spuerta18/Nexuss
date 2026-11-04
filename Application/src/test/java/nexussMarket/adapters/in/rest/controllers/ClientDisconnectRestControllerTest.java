package nexussMarket.adapters.in.rest.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MvcResult;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import nexussMarket.adapters.in.rest.exception.GlobalExceptionHandler;

/**
 * Through the whole Spring MVC stack: when the client has gone away, the
 * request ends without an error body and without a warning or error in the log.
 */
class ClientDisconnectRestControllerTest extends RestControllerTestSupport {

    @Test
    void aClientThatDisconnectedGetsNoErrorBodyAndLeavesNoErrorInTheLog() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.addAppender(events);
        doAnswer(invocation -> {
            throw new ClientAbortException("Broken pipe");
        }).when(buyerPort).listBuyerOrders(any(), any());

        try {
            MvcResult result = mockMvc.perform(get("/api/buyer/orders").header(authorization(), bearer(buyer())))
                    .andReturn();

            assertEquals("", result.getResponse().getContentAsString(), "nothing is written for a gone client");
            assertTrue(events.list.stream().noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.INFO)),
                    () -> "logged: " + events.list);
        } finally {
            logger.detachAppender(events);
        }
    }
}
