package com.km.langgraph4j.support;

import org.bsc.langgraph4j.action.EdgeAction;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A conditional edge is just a function from state to a routing key - test it
 * the same way you'd test any other function, by handing it a state and
 * asserting on the outcome. No graph, no LLM, no Spring context required.
 */
class RoutingTest {

    private final EdgeAction<TicketState> routeByCategory = state ->
            "COMPLEX".equals(state.category()) ? "complex" : "simple";

    private final EdgeAction<TicketState> routeByDecision = state ->
            "APPROVED".equals(state.humanDecision()) ? "approved" : "rejected";

    @Test
    void complexTicketsGoToHumanReview() throws Exception {
        var state = new TicketState(Map.of(TicketState.CATEGORY, "COMPLEX"));
        assertThat(routeByCategory.apply(state)).isEqualTo("complex");
    }

    @Test
    void simpleTicketsAutoResolve() throws Exception {
        var state = new TicketState(Map.of(TicketState.CATEGORY, "SIMPLE"));
        assertThat(routeByCategory.apply(state)).isEqualTo("simple");
    }

    @Test
    void rejectedReviewsAreEscalated() throws Exception {
        var state = new TicketState(Map.of(TicketState.HUMAN_DECISION, "REJECTED"));
        assertThat(routeByDecision.apply(state)).isEqualTo("rejected");
    }
}
