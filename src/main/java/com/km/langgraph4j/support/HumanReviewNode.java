package com.km.langgraph4j.support;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The graph is compiled with {@code interruptBefore("human_review")}, so
 * execution pauses right before this node runs and control returns to the
 * caller (see TicketGraphConfig).
 *
 * <p>{@code apply()} below is <b>never actually invoked</b> in the current
 * flow: {@code TicketController.decide()} calls
 * {@code updateState(config, decisionUpdate, "human_review")}, which writes
 * the decision as if this node had already produced it, then resumes
 * straight from human_review's outgoing edge — skipping this method's body
 * entirely. It's kept for graph-structure completeness (this node still
 * needs to exist to be an interrupt target and an edge source), not because
 * its logic runs.
 */
@Component
public class HumanReviewNode implements NodeAction<TicketState> {

    @Override
    public Map<String, Object> apply(TicketState state) {
        return Map.of(TicketState.MESSAGES, "human_review -> decision=" + state.humanDecision());
    }
}
