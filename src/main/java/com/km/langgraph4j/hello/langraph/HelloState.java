package com.km.langgraph4j.hello.langraph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The "Hello World" graph state — this is the exercise starting point.
 *
 * Just one field: a running transcript that every node appends to.
 * {@link Channels#appender} tells LangGraph4j to merge updates by appending
 * to the list rather than overwriting it.
 */
public class HelloState extends AgentState {

    public static final String MESSAGES_KEY = "messages";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            MESSAGES_KEY, Channels.appender(ArrayList::new)
    );

    public HelloState(Map<String, Object> initData) {
        super(initData);
    }

    public List<String> messages() {
        return this.<List<String>>value(MESSAGES_KEY).orElse(List.of());
    }
}
