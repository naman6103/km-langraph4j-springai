package com.km.langgraph4j.support;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Shared state for the IT-support ticket triage graph.
 *
 * Only "messages" needs a custom reducer — every new value is appended to the
 * running transcript. Every other key (category, resolution, status,
 * humanDecision) is a plain scalar: LangGraph4j's default "last write wins"
 * behaviour is exactly what we want for those.
 */
public class TicketState extends AgentState {

    public static final String TICKET_TEXT    = "ticketText";
    public static final String MESSAGES       = "messages";
    public static final String CATEGORY       = "category";       // SIMPLE | COMPLEX
    public static final String RESOLUTION     = "resolution";
    public static final String STATUS         = "status";         // NEW | AWAITING_REVIEW | RESOLVED | ESCALATED
    public static final String HUMAN_DECISION = "humanDecision";  // APPROVED | REJECTED

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            MESSAGES, Channels.appender(ArrayList::new)
    );

    public TicketState(Map<String, Object> initData) {
        super(initData);
    }

    public String ticketText() {
        return this.<String>value(TICKET_TEXT).orElse("");
    }

    public List<String> messages() {
        return this.<List<String>>value(MESSAGES).orElse(List.of());
    }

    public String category() {
        return this.<String>value(CATEGORY).orElse("UNKNOWN");
    }

    public String status() {
        return this.<String>value(STATUS).orElse("NEW");
    }

    public Optional<String> resolution() {
        return this.value(RESOLUTION);
    }

    public String humanDecision() {
        return this.<String>value(HUMAN_DECISION).orElse(null);
    }
}
