package com.km.langgraph4j.exception;

import java.io.Serial;

/**
 * Signals that no checkpointed ticket exists for the requested id.
 *
 * <p>Thrown when a decision is submitted for a {@code ticketId} that was
 * never created, or whose checkpoint is no longer present in the checkpoint
 * store (for example, after an application restart with the in-memory
 * {@link org.bsc.langgraph4j.checkpoint.MemorySaver}).
 */
public class TicketNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TicketNotFoundException(String ticketId, Throwable cause) {
        super("No ticket awaiting review for id " + ticketId, cause);
    }
}
