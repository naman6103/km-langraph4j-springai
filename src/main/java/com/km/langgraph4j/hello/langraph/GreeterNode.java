package com.km.langgraph4j.hello.langraph;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * A node is just a function of state: read the current state in,
 * return a partial update out. No LLM call needed to prove the wiring works.
 */
@Component
public class GreeterNode implements NodeAction<HelloState> {

    @Override
    public Map<String, Object> apply(HelloState state) {
        return Map.of(HelloState.MESSAGES_KEY, "Hello from GreeterNode!");
    }
}
