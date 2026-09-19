package com.km.langgraph4j.support;

import com.km.langgraph4j.config.GraphObservability;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 *                              ┌── simple ──► auto_resolve ─┐
 *  START ─► classify ─────────┤                             ├─► finalize ─► END
 *                              └── complex ─► human_review ──┼── approved ──┘
 *                                             (⏸ interrupt)  └── rejected ─► escalate ─► END
 *
 * classify and human_review are conditional edges (addConditionalEdges);
 * everything else is a normal edge (addEdge). The graph is compiled with a
 * CheckpointSaver + interruptBefore("human_review"), so a COMPLEX ticket
 * pauses — durably, not just in a thread that's blocked — until a human
 * calls POST /api/tickets/{id}/decision.
 */
@Configuration
public class TicketGraphConfig {

    private static final Logger log = LoggerFactory.getLogger(TicketGraphConfig.class);

    /**
     * In-memory checkpoint store — perfect for this workshop, gone on restart.
     * Swap for langgraph4j-postgres-saver / -redis-saver / -mysql-saver in
     * production without touching any node or edge code: only this bean changes.
     */
    @Bean
    public MemorySaver ticketCheckpointSaver() {
        return new MemorySaver();
    }

    @Bean
    public CompiledGraph<TicketState> ticketGraph(
            ClassifyNode classifyNode,
            AutoResolveNode autoResolveNode,
            HumanReviewNode humanReviewNode,
            EscalateNode escalateNode,
            FinalizeNode finalizeNode,
            MemorySaver ticketCheckpointSaver,
            @Value("${app.observability.ticket.enabled:true}") boolean observabilityEnabled
    ) throws GraphStateException {

        EdgeAction<TicketState> routeByCategory = state ->
                "COMPLEX".equals(state.category()) ? "complex" : "simple";

        EdgeAction<TicketState> routeByDecision = state ->
                "APPROVED".equals(state.humanDecision()) ? "approved" : "rejected";

        var graph = new StateGraph<>(TicketState.SCHEMA, TicketState::new)
                .addNode("classify", node_async(classifyNode))
                .addNode("auto_resolve", node_async(autoResolveNode))
                .addNode("human_review", node_async(humanReviewNode))
                .addNode("escalate", node_async(escalateNode))
                .addNode("finalize", node_async(finalizeNode))

                .addEdge(START, "classify")
                .addConditionalEdges("classify", edge_async(routeByCategory),
                        Map.of("simple", "auto_resolve", "complex", "human_review"))
                .addConditionalEdges("human_review", edge_async(routeByDecision),
                        Map.of("approved", "finalize", "rejected", "escalate"))
                .addEdge("auto_resolve", "finalize")
                .addEdge("escalate", END)
                .addEdge("finalize", END);

        if (observabilityEnabled) {
            graph.addWrapCallNodeHook(GraphObservability.nodeTimingHook(log))
                    .addWrapCallEdgeHook(GraphObservability.edgeRoutingHook(log));
        }

        var compileConfig = CompileConfig.builder()
                .checkpointSaver(ticketCheckpointSaver)
                // Pause right BEFORE human_review runs: state is checkpointed,
                // control returns to the HTTP caller, thread is not released.
                .interruptBefore("human_review")
                .releaseThread(false)
                .build();

        return graph.compile(compileConfig);
    }
}
