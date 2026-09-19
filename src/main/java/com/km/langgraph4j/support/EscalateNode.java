package com.km.langgraph4j.support;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EscalateNode implements NodeAction<TicketState> {

    @Override
    public Map<String, Object> apply(TicketState state) {
        return Map.of(
                TicketState.STATUS, "ESCALATED",
                TicketState.MESSAGES, "escalate -> routed to human agent queue"
        );
    }
}
