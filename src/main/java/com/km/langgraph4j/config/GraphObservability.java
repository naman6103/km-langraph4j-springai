package com.km.langgraph4j.config;

import org.bsc.langgraph4j.action.AsyncCommandAction;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.hook.EdgeHook;
import org.bsc.langgraph4j.hook.NodeHook;
import org.bsc.langgraph4j.state.AgentState;
import org.slf4j.Logger;

/**
 * Registered once per graph via {@code StateGraph#addWrapCallNodeHook} /
 * {@code #addWrapCallEdgeHook}, these wrap every node call and every
 * conditional-edge routing decision with timing + logging. This is the
 * extension point langgraph4j-core:1.8.24 actually provides for
 * instrumentation - {@link NodeHook.WrapCall#applyWrap} wraps the real
 * {@link AsyncNodeActionWithConfig}, {@link EdgeHook.WrapCall#applyWrap}
 * wraps the real {@link AsyncCommandAction} - so no node or router needs a
 * log line of its own, and a Micrometer Timer.sample()/.stop() (or a tracing
 * span) would slot into the same two lambdas.
 */
public final class GraphObservability {

    private GraphObservability() {
    }

    public static <S extends AgentState> NodeHook.WrapCall<S> nodeTimingHook(Logger log) {
        return (nodeId, state, config, action) -> {
            String threadId = config.threadId().orElse("-");
            long start = System.nanoTime();
            log.info("node={} threadId={} start", nodeId, threadId);
            return action.apply(state, config).whenComplete((result, ex) -> {
                long tookMs = (System.nanoTime() - start) / 1_000_000;
                if (ex != null) {
                    log.warn("node={} threadId={} failed after {}ms", nodeId, threadId, tookMs, ex);
                } else {
                    log.info("node={} threadId={} done in {}ms", nodeId, threadId, tookMs);
                }
            });
        };
    }

    public static <S extends AgentState> EdgeHook.WrapCall<S> edgeRoutingHook(Logger log) {
        return (fromNode, state, config, action) -> {
            String threadId = config.threadId().orElse("-");
            return action.apply(state, config).whenComplete((command, ex) -> {
                if (ex != null) {
                    log.warn("route from={} threadId={} failed", fromNode, threadId, ex);
                } else {
                    log.info("route from={} threadId={} -> {}", fromNode, threadId, command.gotoNode());
                }
            });
        };
    }
}
