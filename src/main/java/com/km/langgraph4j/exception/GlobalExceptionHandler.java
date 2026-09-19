package com.km.langgraph4j.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralizes translation of domain exceptions into HTTP error responses so
 * individual controllers stay free of error-formatting logic.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Translates a {@link TicketNotFoundException} into a {@code 404 Not Found} response.
     *
     * @param ex the exception carrying the missing ticket's id; must not be {@code null}
     * @return a {@link ProblemDetail} with HTTP status {@code 404 Not Found} and
     *     {@code ex}'s message as the detail
     */
    @ExceptionHandler(TicketNotFoundException.class)
    public ProblemDetail handleTicketNotFound(TicketNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
