package com.km.langgraph4j.hello.langraph;

import com.km.langgraph4j.config.GraphObservability;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * START -> greeter -> responder -> END
 *
 * This is the graph the exercise starts from. Once it's running, try:
 *   1. Add a third node and wire it in.
 *   2. Replace the straight edge after "greeter" with addConditionalEdges(...).
 *   3. Give the graph a CheckpointSaver so a second call can continue thread.
 * See EXERCISE.md for the full step-by-step version.
 */
@Configuration
public class HelloGraphConfig {

    private static final Logger log = LoggerFactory.getLogger(HelloGraphConfig.class);

    @Bean
    public CompiledGraph<HelloState> helloGraph(
            GreeterNode greeterNode,
            ResponderNode responderNode,
            @Value("${app.observability.hello.enabled:true}") boolean observabilityEnabled) throws GraphStateException {

        var stateGraph = new StateGraph<>(HelloState.SCHEMA, HelloState::new)
                .addNode("greeter", node_async(greeterNode))
                .addNode("responder", node_async(responderNode))
                .addEdge(START, "greeter")
                .addEdge("greeter", "responder")
                .addEdge("responder", END);

        if (observabilityEnabled) {
            stateGraph.addWrapCallNodeHook(GraphObservability.nodeTimingHook(log));
        }

        return stateGraph.compile();
    }
}
