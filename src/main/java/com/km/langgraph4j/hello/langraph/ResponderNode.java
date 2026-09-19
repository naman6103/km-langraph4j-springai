package com.km.langgraph4j.hello.langraph;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ResponderNode implements NodeAction<HelloState> {

    @Override
    public Map<String, Object> apply(HelloState state) {
        if (state.messages().contains("Hello from GreeterNode!")) {
            return Map.of(HelloState.MESSAGES_KEY, "Acknowledged greeting!");
        }
        return Map.of(HelloState.MESSAGES_KEY, "No greeting found.");
    }
}
