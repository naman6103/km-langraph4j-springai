package com.km.langgraph4j.support;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FinalizeNode implements NodeAction<TicketState> {

    @Override
    public Map<String, Object> apply(TicketState state) {
        String resolution = state.resolution()
                .orElse("Reviewed and approved by a human agent.");

        return Map.of(
                TicketState.STATUS, "RESOLVED",
                TicketState.RESOLUTION, resolution,
                TicketState.MESSAGES, "finalize -> ticket resolved"
        );
    }
}
