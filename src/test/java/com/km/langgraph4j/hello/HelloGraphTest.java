package com.km.langgraph4j.hello;

import com.km.langgraph4j.hello.langraph.GreeterNode;
import com.km.langgraph4j.hello.langraph.HelloState;
import com.km.langgraph4j.hello.langraph.ResponderNode;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Nodes are plain functions of state, so the whole graph can be built and run
 * in a plain JUnit test - no Spring context, no LLM, no network. This is the
 * cheapest, fastest layer of the testing pyramid for a graph.
 */
class HelloGraphTest {

    @Test
    void responderAcknowledgesTheGreeting() throws Exception {
        var stateGraph = new StateGraph<>(HelloState.SCHEMA, HelloState::new)
                .addNode("greeter", node_async(new GreeterNode()))
                .addNode("responder", node_async(new ResponderNode()))
                .addEdge(START, "greeter")
                .addEdge("greeter", "responder")
                .addEdge("responder", END);

        CompiledGraph<HelloState> graph = stateGraph.compile();
        var config = RunnableConfig.builder().threadId(UUID.randomUUID().toString()).build();

        HelloState result = graph
                .invoke(Map.<String, Object>of(HelloState.MESSAGES_KEY, "Let's begin!"), config)
                .orElseThrow();

        List<String> messages = result.messages();

        assertThat(messages).contains("Hello from GreeterNode!", "Acknowledged greeting!");
    }
}
