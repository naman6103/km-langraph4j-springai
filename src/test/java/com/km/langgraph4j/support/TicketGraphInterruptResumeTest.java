package com.km.langgraph4j.support;

import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Reproduces the exact interrupt/resume flow {@code TicketController.decide()}
 * uses, with the real {@link HumanReviewNode}, {@link EscalateNode} and
 * {@link FinalizeNode} (all LLM-free) and a stubbed classify node - no Spring
 * context, no ChatClient required.
 *
 * <p>This pins down a real langgraph4j-core:1.8.24 gotcha: after
 * {@code updateState(config, values, "human_review")}, resuming with
 * {@code stream(Map.of(), resumedConfig)} silently restarts the graph from
 * {@code START} instead of continuing - it must be
 * {@code stream(GraphInput.resume(), resumedConfig)}. See "A note on
 * accuracy" in the project README.
 */
class TicketGraphInterruptResumeTest {

    private static CompiledGraph<TicketState> buildComplexPathGraph(MemorySaver saver) throws Exception {
        NodeAction<TicketState> classify = state -> Map.of(
                TicketState.CATEGORY, "COMPLEX",
                TicketState.STATUS, "AWAITING_REVIEW",
                TicketState.MESSAGES, "classify -> COMPLEX");
        NodeAction<TicketState> autoResolve = state -> Map.of();

        EdgeAction<TicketState> routeByCategory = state ->
                "COMPLEX".equals(state.category()) ? "complex" : "simple";
        EdgeAction<TicketState> routeByDecision = state ->
                "APPROVED".equals(state.humanDecision()) ? "approved" : "rejected";

        var graph = new StateGraph<>(TicketState.SCHEMA, TicketState::new)
                .addNode("classify", node_async(classify))
                .addNode("auto_resolve", node_async(autoResolve))
                .addNode("human_review", node_async(new HumanReviewNode()))
                .addNode("escalate", node_async(new EscalateNode()))
                .addNode("finalize", node_async(new FinalizeNode()))
                .addEdge(START, "classify")
                .addConditionalEdges("classify", edge_async(routeByCategory),
                        Map.of("simple", "auto_resolve", "complex", "human_review"))
                .addConditionalEdges("human_review", edge_async(routeByDecision),
                        Map.of("approved", "finalize", "rejected", "escalate"))
                .addEdge("auto_resolve", "finalize")
                .addEdge("escalate", END)
                .addEdge("finalize", END);

        var compileConfig = CompileConfig.builder()
                .checkpointSaver(saver)
                .interruptBefore("human_review")
                .releaseThread(false)
                .build();

        return graph.compile(compileConfig);
    }

    @Test
    void approvingAResumedTicketReachesFinalize() throws Exception {
        var compiled = buildComplexPathGraph(new MemorySaver());
        var config = RunnableConfig.builder().threadId("approve-flow").build();

        TicketState afterClassify = null;
        for (var output : compiled.stream(Map.<String, Object>of(TicketState.TICKET_TEXT, "prod db down"), config)) {
            afterClassify = output.state();
        }
        assertThat(afterClassify.status()).isEqualTo("AWAITING_REVIEW");

        var resumedConfig = compiled.updateState(config,
                Map.<String, Object>of(TicketState.HUMAN_DECISION, "APPROVED"), "human_review");

        TicketState resolved = null;
        for (var output : compiled.stream(GraphInput.resume(), resumedConfig)) {
            resolved = output.state();
        }

        assertThat(resolved.status()).isEqualTo("RESOLVED");
        assertThat(resolved.messages()).containsExactly("classify -> COMPLEX", "finalize -> ticket resolved");
    }

    @Test
    void rejectingAResumedTicketReachesEscalate() throws Exception {
        var compiled = buildComplexPathGraph(new MemorySaver());
        var config = RunnableConfig.builder().threadId("reject-flow").build();

        for (var output : compiled.stream(Map.<String, Object>of(TicketState.TICKET_TEXT, "prod db down"), config)) {
            // drain to the interrupt point
        }

        var resumedConfig = compiled.updateState(config,
                Map.<String, Object>of(TicketState.HUMAN_DECISION, "REJECTED"), "human_review");

        TicketState escalated = null;
        for (var output : compiled.stream(GraphInput.resume(), resumedConfig)) {
            escalated = output.state();
        }

        assertThat(escalated.status()).isEqualTo("ESCALATED");
        assertThat(escalated.messages()).containsExactly("classify -> COMPLEX", "escalate -> routed to human agent queue");
    }
}
